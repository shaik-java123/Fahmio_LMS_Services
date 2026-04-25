package com.lms.service;

import com.lms.model.*;
import com.lms.repository.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final CourseRepository courseRepository;
    private final CoursePriceRepository coursePriceRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EmailService emailService;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    /**
     * Create a Stripe Checkout Session for purchasing a course.
     * Returns the session URL for the frontend to redirect to.
     */
    @Transactional
    public String createCheckoutSession(Long courseId, Long userId) throws StripeException {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CoursePrice price = coursePriceRepository.findByCourseId(courseId)
                .orElseGet(() -> {
                    CoursePrice cp = new CoursePrice();
                    cp.setCourse(course);
                    cp.setType((course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) == 0) ? CoursePrice.PriceType.FREE : CoursePrice.PriceType.ONE_TIME);
                    cp.setAmount(course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO);
                    cp.setCurrency("USD");
                    return coursePriceRepository.save(cp);
                });

        if (price.getType() == CoursePrice.PriceType.FREE) {
            enrollDirectly(user, course, null, null);
            return frontendUrl + "/my-learning?enrolled=true";
        }

        // Check if already purchased
        if (orderRepository.existsByUserIdAndCourseIdAndStatus(
                userId, courseId, Order.OrderStatus.COMPLETED)) {
            return frontendUrl + "/my-learning?already-enrolled=true";
        }

        BigDecimal amount = (price.getAmount() != null) ? price.getAmount() : BigDecimal.ZERO;
        long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        // ONLY trigger bypass if it is the literal placeholder, NOT if it's a real sk_test key.
        if (stripeSecretKey == null || stripeSecretKey.equals("sk_test_YOUR_STRIPE_SECRET_KEY") || !stripeSecretKey.startsWith("sk_")) {
            log.warn("Simulation Mode: Dummy Stripe API key detected. Auto-enrolling user.");
            enrollDirectly(user, course, null, null);
            
            Order dummyOrder = new Order();
            dummyOrder.setUser(user);
            dummyOrder.setCourse(course);
            dummyOrder.setAmount(price.getAmount());
            dummyOrder.setCurrency(price.getCurrency());
            dummyOrder.setStatus(Order.OrderStatus.COMPLETED);
            dummyOrder.setStripeSessionId("simulated_session_" + System.currentTimeMillis());
            orderRepository.save(dummyOrder);
            
            return frontendUrl + "/my-learning?enrolled=true&simulated=true";
        }

        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setMode(price.getType() == CoursePrice.PriceType.SUBSCRIPTION 
                        ? SessionCreateParams.Mode.SUBSCRIPTION 
                        : SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(user.getEmail())
                .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/payment/cancel")
                .putMetadata("courseId", courseId.toString())
                .putMetadata("userId", userId.toString())
                .putMetadata("tenantId", user.getTenant() != null ? user.getTenant().getSubdomain() : "global");

        if (price.getStripePriceId() != null && !price.getStripePriceId().isEmpty()) {
            sessionBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPrice(price.getStripePriceId())
                    .build());
        } else {
            sessionBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(price.getCurrency().toLowerCase())
                            .setUnitAmount(amountInCents)
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(course.getTitle())
                                    .setDescription(course.getDescription() != null
                                            ? course.getDescription().substring(0, Math.min(500, course.getDescription().length()))
                                            : "")
                                    .build())
                            .setRecurring(price.getType() == CoursePrice.PriceType.SUBSCRIPTION
                                    ? SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                            .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                            .build()
                                    : null)
                            .build())
                    .build());
        }

        Session session = Session.create(sessionBuilder.build());

        // Create a pending order for tracking
        Order pendingOrder = new Order();
        pendingOrder.setUser(user);
        pendingOrder.setCourse(course);
        pendingOrder.setAmount(price.getAmount());
        pendingOrder.setCurrency(price.getCurrency());
        pendingOrder.setStatus(Order.OrderStatus.PENDING);
        pendingOrder.setStripeSessionId(session.getId());
        orderRepository.save(pendingOrder);

        return session.getUrl();
    }

    /**
     * Manually verify a Stripe Checkout Session status (Fallback for failed webhooks).
     */
    @Transactional
    public void verifySession(String sessionId) throws StripeException {
        log.info("Manually verifying Stripe Session: {}", sessionId);
        // Expand line_items so we can see product details if needed
        Session session = Session.retrieve(sessionId);
        
        log.info("Session {} paymentStatus={} status={}", sessionId, session.getPaymentStatus(), session.getStatus());
        
        if ("paid".equals(session.getPaymentStatus()) || "complete".equals(session.getStatus())) {
            log.info("Session {} is PAID. Completing enrollment.", sessionId);
            handleCheckoutCompleted(session);
        } else {
            log.warn("Session {} is NOT paid. paymentStatus={}", sessionId, session.getPaymentStatus());
            throw new RuntimeException("Payment not completed yet. Please wait a moment.");
        }
    }

    /**
     * Create a Razorpay Order for purchasing a course.
     */
    @Transactional
    public java.util.Map<String, Object> createRazorpayOrder(Long courseId, Long userId) throws Exception {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CoursePrice price = coursePriceRepository.findByCourseId(courseId)
                .orElseGet(() -> {
                    CoursePrice cp = new CoursePrice();
                    cp.setCourse(course);
                    cp.setType(course.getPrice().compareTo(BigDecimal.ZERO) == 0 ? CoursePrice.PriceType.FREE : CoursePrice.PriceType.ONE_TIME);
                    cp.setAmount(course.getPrice());
                    cp.setCurrency("INR");
                    return coursePriceRepository.save(cp);
                });

        long amountInPaise = price.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        if (razorpayKeyId == null || razorpayKeyId.contains("YOUR_RAZORPAY_")) {
             log.warn("Mock Razorpay mode. Bypassing real order creation.");
             Order dummy = new Order();
             dummy.setUser(user);
             dummy.setCourse(course);
             dummy.setAmount(price.getAmount());
             dummy.setCurrency("INR");
             dummy.setStatus(Order.OrderStatus.PENDING);
             dummy.setRazorpayOrderId("order_mock_" + System.currentTimeMillis());
             orderRepository.save(dummy);

             return java.util.Map.of(
                 "id", dummy.getRazorpayOrderId(),
                 "amount", amountInPaise,
                 "currency", "INR",
                 "mock", true
             );
        }

        com.razorpay.RazorpayClient client = new com.razorpay.RazorpayClient(razorpayKeyId, razorpayKeySecret);
        
        org.json.JSONObject orderRequest = new org.json.JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis());
        
        com.razorpay.Order razorpayOrder = client.orders.create(orderRequest);
        
        Order pending = new Order();
        pending.setUser(user);
        pending.setCourse(course);
        pending.setAmount(price.getAmount());
        pending.setCurrency("INR");
        pending.setStatus(Order.OrderStatus.PENDING);
        pending.setRazorpayOrderId(razorpayOrder.get("id"));
        orderRepository.save(pending);

        return java.util.Map.of(
            "id", razorpayOrder.get("id"),
            "amount", (Integer) razorpayOrder.get("amount"),
            "currency", (String) razorpayOrder.get("currency")
        );
    }

    @Transactional
    public void verifyRazorpayPayment(String razorpayOrderId, String razorpayPaymentId, String signature) throws Exception {
        if (razorpayKeyId != null && razorpayKeyId.contains("YOUR_RAZORPAY_")) {
            log.info("Verifying mock Razorpay payment for order {}", razorpayOrderId);
            completeRazorpayOrder(razorpayOrderId, razorpayPaymentId);
            return;
        }

        boolean isValid = com.razorpay.Utils.verifyPaymentSignature(new org.json.JSONObject(java.util.Map.of(
            "razorpay_order_id", razorpayOrderId,
            "razorpay_payment_id", razorpayPaymentId,
            "razorpay_signature", signature
        )), razorpayKeySecret);

        if (isValid) {
            completeRazorpayOrder(razorpayOrderId, razorpayPaymentId);
        } else {
            throw new RuntimeException("Invalid Razorpay payment signature");
        }
    }

    private void completeRazorpayOrder(String razorpayOrderId, String razorpayPaymentId) {
        orderRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setRazorpayPaymentId(razorpayPaymentId);
            orderRepository.save(order);
            enrollDirectly(order.getUser(), order.getCourse(), null, null);
        });
    }

     public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    /**
     * Handle Stripe webhook events (checkout.session.completed, customer.subscription.deleted).
     */
    @Transactional
    public void handleWebhook(String payload, String sigHeader) throws StripeException {
        com.stripe.model.Event event = com.stripe.net.Webhook.constructEvent(
                payload, sigHeader, webhookSecret);

        log.info("Processing Stripe Webhook: {}", event.getType());
        
        // Before processing, try to set the correct tenant context from session metadata
        if (event.getDataObjectDeserializer().getObject().orElse(null) instanceof Session session) {
            String tenantId = session.getMetadata().get("tenantId");
            if (tenantId != null) {
                com.lms.tenant.TenantContext.setCurrentTenant(tenantId);
                log.info("Set tenant context to {} for event {}", tenantId, event.getType());
            }
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> {
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                handleCheckoutCompleted(session);
            }
            case "customer.subscription.deleted" -> {
                com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                handleSubscriptionDeleted(subscription);
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent pi = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                handlePaymentFailed(pi);
            }
            default -> log.info("Unhandled Stripe event: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Session session) {
        String tenantId = session.getMetadata().get("tenantId");
        if (tenantId != null) {
            com.lms.tenant.TenantContext.setCurrentTenant(tenantId);
        }
        
        // First, try to find the pending order we created
        Optional<Order> existingOrder = orderRepository.findByStripeSessionId(session.getId());
        
        if (existingOrder.isPresent()) {
            Order order = existingOrder.get();
            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setStripePaymentIntentId(session.getPaymentIntent());
            orderRepository.save(order);
            
            String subId = session.getSubscription();
            enrollDirectly(order.getUser(), order.getCourse(), subId, null);
            log.info("Enrollment completed via existing order for session {}", session.getId());
        } else {
            // Fallback: use session metadata to look up user+course directly
            log.warn("No pending order found for session {}. Attempting metadata fallback.", session.getId());
            Map<String, String> metadata = session.getMetadata();
            if (metadata == null || !metadata.containsKey("courseId") || !metadata.containsKey("userId")) {
                log.error("Session {} has no metadata. Cannot complete enrollment.", session.getId());
                return;
            }
            
            Long courseId = Long.parseLong(metadata.get("courseId"));
            Long userId = Long.parseLong(metadata.get("userId"));
            
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            // Create the order now
            CoursePrice price = coursePriceRepository.findByCourseId(courseId).orElse(null);
            Order order = new Order();
            order.setUser(user);
            order.setCourse(course);
            order.setAmount(price != null ? price.getAmount() : BigDecimal.ZERO);
            order.setCurrency(price != null ? price.getCurrency() : "USD");
            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setStripeSessionId(session.getId());
            order.setStripePaymentIntentId(session.getPaymentIntent());
            orderRepository.save(order);
            
            String subId = session.getSubscription();
            enrollDirectly(user, course, subId, null);
            log.info("Enrollment completed via metadata fallback for session {}", session.getId());
        }
    }

    private void handleSubscriptionDeleted(com.stripe.model.Subscription subscription) {
        enrollmentRepository.findByStripeSubscriptionId(subscription.getId()).ifPresent(enrollment -> {
            enrollment.setStatus(Enrollment.Status.EXPIRED);
            enrollmentRepository.save(enrollment);
            log.info("Subscription {} expired for user {}. Course access revoked.", 
                subscription.getId(), enrollment.getStudent().getEmail());
        });
    }

    private void handlePaymentFailed(PaymentIntent pi) {
        orderRepository.findByStripePaymentIntentId(pi.getId()).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.FAILED);
            orderRepository.save(order);
        });
    }

    private void enrollDirectly(User user, Course course, String subId, LocalDateTime validUntil) {
        // Use native query to find existing enrollments even if they belong to 'global' or 'null' tenant
        Optional<Enrollment> existing = enrollmentRepository.findByStudentAndCourseIgnoreFilter(
                user.getId(), course.getId());
        
        Enrollment enrollment;
        if (existing.isPresent()) {
            enrollment = existing.get();
            log.info("Found existing enrollment for {} in course {}. Re-activating and setting correct tenant.", user.getEmail(), course.getTitle());
        } else {
            enrollment = new Enrollment();
            enrollment.setStudent(user);
            enrollment.setCourse(course);
            enrollment.setTenant(course.getTenant());
            enrollment.setProgress(0);
        }

        enrollment.setStatus(Enrollment.Status.ACTIVE);
        enrollment.setTenant(course.getTenant());
        enrollment.setStripeSubscriptionId(subId);
        enrollment.setValidUntil(validUntil);
        
        try {
            enrollmentRepository.save(enrollment);
            log.info("User {} enrolled in course {}. SubID: {}", user.getEmail(), course.getTitle(), subId);
            emailService.sendEnrollmentConfirmation(
                    user.getEmail(), user.getFirstName(), course.getTitle());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Enrollment race condition detected for user {} and course {}. Another thread already created the enrollment.", user.getEmail(), course.getTitle());
            // It's safe to ignore as the other thread successfully created the enrollment
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public CoursePrice setCoursePrice(Long courseId, CoursePrice.PriceType type,
                                      BigDecimal amount, String currency) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        CoursePrice price = coursePriceRepository.findByCourseId(courseId)
                .orElse(new CoursePrice());
        price.setCourse(course);
        price.setType(type);
        price.setAmount(type == CoursePrice.PriceType.FREE ? BigDecimal.ZERO : amount);
        price.setCurrency(currency != null ? currency : "USD");
        return coursePriceRepository.save(price);
    }
}
