package com.lms.controller;

import com.lms.security.JwtUtil;
import com.lms.service.PaymentService;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Stripe Checkout and Webhooks")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtUtil jwtUtil;

    @PostMapping("/checkout/{courseId}")
    @Operation(summary = "Build Stripe Checkout Session for a course purchase")
    public ResponseEntity<Map<String, String>> createCheckout(
            @PathVariable Long courseId,
            @RequestHeader("Authorization") String authHeader) throws StripeException {
        
        Long userId = extractUserId(authHeader);
        String checkoutUrl = paymentService.createCheckoutSession(courseId, userId);
        
        return ResponseEntity.ok(Map.of("url", checkoutUrl));
    }

    @PostMapping("/razorpay/order/{courseId}")
    @Operation(summary = "Create Razorpay Order for a course purchase")
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(
            @PathVariable Long courseId,
            @RequestHeader("Authorization") String authHeader) throws Exception {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(paymentService.createRazorpayOrder(courseId, userId));
    }

    @PostMapping("/razorpay/verify")
    @Operation(summary = "Verify Razorpay Payment Signature")
    public ResponseEntity<Void> verifyRazorpayPayment(
            @RequestBody Map<String, String> payload) throws Exception {
        paymentService.verifyRazorpayPayment(
            payload.get("razorpay_order_id"),
            payload.get("razorpay_payment_id"),
            payload.get("razorpay_signature")
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/razorpay/key")
    @Operation(summary = "Get Razorpay Public Key ID")
    public ResponseEntity<Map<String, String>> getRazorpayKey() {
        return ResponseEntity.ok(Map.of("keyId", paymentService.getRazorpayKeyId()));
    }

    @PostMapping("/verify-stripe/{sessionId}")
    @Operation(summary = "Manually check Stripe session and complete order if paid")
    public ResponseEntity<Void> verifyStripeSession(@PathVariable String sessionId) throws StripeException {
        paymentService.verifySession(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook")
    @Operation(summary = "Stripe Webhook Endpoint")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) throws com.stripe.exception.StripeException {
        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/orders")
    @Operation(summary = "Get user order history / invoices")
    public ResponseEntity<java.util.List<com.lms.model.Order>> getOrders(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(paymentService.getUserOrders(userId));
    }

    /** Helper to get User ID from JWT Token */
    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}
