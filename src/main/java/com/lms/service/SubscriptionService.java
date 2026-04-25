package com.lms.service;

import com.lms.dto.SubscriptionPlanDTO;
import com.lms.dto.TenantRegistrationRequest;
import com.lms.dto.TenantSubscriptionDTO;
import com.lms.model.*;
import com.lms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ============ SUBSCRIPTION PLAN MANAGEMENT ============

    /**
     * Get all active subscription plans ordered by display order
     */
    @Transactional(readOnly = true)
    public List<SubscriptionPlanDTO> getAllPlans() {
        return planRepository.findByStatusOrderByDisplayOrder(SubscriptionPlan.PlanStatus.ACTIVE)
                .stream()
                .map(this::convertPlanToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific plan by planKey (e.g., "free", "basic", "business")
     */
    @Transactional(readOnly = true)
    public SubscriptionPlan getPlanByKey(String planKey) {
        return planRepository.findByPlanKey(planKey)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + planKey));
    }

    /**
     * Create a new subscription plan (admin only)
     */
    @Transactional
    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        if (planRepository.existsByPlanKey(plan.getPlanKey())) {
            throw new RuntimeException("Plan with key '" + plan.getPlanKey() + "' already exists");
        }
        return planRepository.save(plan);
    }

    /**
     * Update an existing plan
     */
    @Transactional
    public SubscriptionPlan updatePlan(Long planId, SubscriptionPlan updates) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        if (updates.getName() != null) plan.setName(updates.getName());
        if (updates.getDescription() != null) plan.setDescription(updates.getDescription());
        if (updates.getMonthlyPrice() != null) plan.setMonthlyPrice(updates.getMonthlyPrice());
        if (updates.getAnnualPrice() != null) plan.setAnnualPrice(updates.getAnnualPrice());
        if (updates.getMaxStudents() != null) plan.setMaxStudents(updates.getMaxStudents());
        if (updates.getMaxCourses() != null) plan.setMaxCourses(updates.getMaxCourses());
        if (updates.getMaxInstructors() != null) plan.setMaxInstructors(updates.getMaxInstructors());
        if (updates.getMaxStorageGB() != null) plan.setMaxStorageGB(updates.getMaxStorageGB());
        if (updates.getCustomDomain() != null) plan.setCustomDomain(updates.getCustomDomain());
        if (updates.getCustomBranding() != null) plan.setCustomBranding(updates.getCustomBranding());
        if (updates.getAdvancedAnalytics() != null) plan.setAdvancedAnalytics(updates.getAdvancedAnalytics());
        if (updates.getApiAccess() != null) plan.setApiAccess(updates.getApiAccess());
        if (updates.getSso() != null) plan.setSso(updates.getSso());
        if (updates.getSupport24x7() != null) plan.setSupport24x7(updates.getSupport24x7());
        if (updates.getDedicatedAccount() != null) plan.setDedicatedAccount(updates.getDedicatedAccount());

        return planRepository.save(plan);
    }

    // ============ TENANT SUBSCRIPTION MANAGEMENT ============

    /**
     * Register a new tenant with subscription
     * Creates tenant, admin user, and assigns subscription plan
     */
    @Transactional
    public TenantSubscriptionDTO registerTenantWithSubscription(TenantRegistrationRequest request) {
        // Validate subdomain
        if (tenantRepository.existsBySubdomain(request.getSubdomain())) {
            throw new RuntimeException("Subdomain already taken: " + request.getSubdomain());
        }

        // Get subscription plan
        SubscriptionPlan plan = getPlanByKey(request.getPlanKey());

        // Create tenant
        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setSubdomain(request.getSubdomain());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setCustomDomain(request.getCustomDomain());
        tenant.setLogoUrl(request.getLogoUrl());
        tenant.setPrimaryColor(request.getPrimaryColor() != null ? request.getPrimaryColor() : "#4f46e5");
        tenant.setAccentColor(request.getAccentColor() != null ? request.getAccentColor() : "#db2777");
        tenant.setTagline(request.getTagline());
        tenant.setPlan(plan.getPlanKey().equals("starter") ? Tenant.Plan.STARTER :
                       plan.getPlanKey().equals("growth") ? Tenant.Plan.GROWTH :
                       plan.getPlanKey().equals("business") ? Tenant.Plan.BUSINESS :
                       Tenant.Plan.ENTERPRISE);
        tenant.setStatus(Tenant.Status.ACTIVE);

        tenant = tenantRepository.save(tenant);
        log.info("Created tenant: {}", tenant.getSubdomain());

        // Create subscription
        TenantSubscription.BillingCycle billingCycle = TenantSubscription.BillingCycle.valueOf(
                request.getBillingCycle() != null ? request.getBillingCycle() : "MONTHLY"
        );

        BigDecimal currentPrice = billingCycle == TenantSubscription.BillingCycle.MONTHLY
                ? plan.getMonthlyPrice()
                : plan.getAnnualPrice();

        TenantSubscription subscription = new TenantSubscription();
        subscription.setTenant(tenant);
        subscription.setPlan(plan);
        subscription.setBillingCycle(billingCycle);
        subscription.setCurrentPrice(currentPrice);
        subscription.setCurrency(plan.getCurrency());
        subscription.setStatus(TenantSubscription.SubscriptionStatus.ACTIVE);
        subscription.setSubscribedAt(LocalDateTime.now());
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(calculateNextRenewalDate(billingCycle));
        subscription.setAutoRenew(true);

        subscription = subscriptionRepository.save(subscription);
        log.info("Created subscription for tenant {} with plan {}", tenant.getSubdomain(), plan.getPlanKey());

        // Create admin user for this tenant
        createAdminUser(tenant, request);

        return convertSubscriptionToDTO(subscription);
    }

    /**
     * Subscribe a tenant to a plan
     */
    @Transactional
    public TenantSubscription subscribeToPlan(Long tenantId, String planKey, String billingCycle) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        SubscriptionPlan plan = getPlanByKey(planKey);

        // Check if already subscribed
        subscriptionRepository.findByTenantId(tenantId).ifPresent(existing -> {
            throw new RuntimeException("Tenant already has an active subscription");
        });

        TenantSubscription.BillingCycle cycle = TenantSubscription.BillingCycle.valueOf(billingCycle);
        BigDecimal price = cycle == TenantSubscription.BillingCycle.MONTHLY
                ? plan.getMonthlyPrice()
                : plan.getAnnualPrice();

        TenantSubscription subscription = new TenantSubscription();
        subscription.setTenant(tenant);
        subscription.setPlan(plan);
        subscription.setBillingCycle(cycle);
        subscription.setCurrentPrice(price);
        subscription.setCurrency(plan.getCurrency());
        subscription.setStatus(TenantSubscription.SubscriptionStatus.ACTIVE);
        subscription.setSubscribedAt(LocalDateTime.now());
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(calculateNextRenewalDate(cycle));
        subscription.setAutoRenew(true);

        return subscriptionRepository.save(subscription);
    }

    /**
     * Upgrade or downgrade tenant plan
     */
    @Transactional
    public TenantSubscription changePlan(Long tenantId, String newPlanKey, String billingCycle) {
        // Verify tenant exists
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("No subscription found for tenant"));

        if (!subscription.canUpgrade() && !subscription.canDowngrade()) {
            throw new RuntimeException("Cannot change plan for subscription with status: " + subscription.getStatus());
        }

        SubscriptionPlan newPlan = getPlanByKey(newPlanKey);
        TenantSubscription.BillingCycle cycle = TenantSubscription.BillingCycle.valueOf(billingCycle);

        // Calculate pro-rata credit for remaining period (if upgrading mid-cycle)
        BigDecimal newPrice = cycle == TenantSubscription.BillingCycle.MONTHLY
                ? newPlan.getMonthlyPrice()
                : newPlan.getAnnualPrice();

        subscription.setPlan(newPlan);
        subscription.setBillingCycle(cycle);
        subscription.setCurrentPrice(newPrice);

        return subscriptionRepository.save(subscription);
    }

    /**
     * Cancel subscription
     */
    @Transactional
    public void cancelSubscription(Long tenantId, String reason) {
        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("No subscription found for tenant"));

        subscription.setStatus(TenantSubscription.SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setCancellationReason(reason);
        subscription.setAutoRenew(false);

        subscriptionRepository.save(subscription);
        log.info("Cancelled subscription for tenant {} with reason: {}", tenantId, reason);
    }

    /**
     * Get tenant subscription details
     */
    @Transactional(readOnly = true)
    public TenantSubscriptionDTO getTenantSubscription(Long tenantId) {
        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("No subscription found for tenant"));

        return convertSubscriptionToDTO(subscription);
    }

    /**
     * Check if tenant can perform action based on plan limits
     */
    @Transactional(readOnly = true)
    public boolean canAddStudent(Long tenantId) {
        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("No subscription found for tenant"));

        return subscription.canAddStudent();
    }

    /**
     * Check if tenant can add course
     */
    @Transactional(readOnly = true)
    public boolean canAddCourse(Long tenantId) {
        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("No subscription found for tenant"));

        return subscription.canAddCourse();
    }

    /**
     * Check if tenant can add instructor
     */
    @Transactional(readOnly = true)
    public boolean canAddInstructor(Long tenantId) {
        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("No subscription found for tenant"));

        return subscription.canAddInstructor();
    }

    /**
     * Update usage metrics
     */
    @Transactional
    public void updateUsageMetrics(Long tenantId, Integer students, Integer courses, Integer instructors, Long storageGB) {
        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("No subscription found for tenant"));

        if (students != null) subscription.setCurrentStudents(students);
        if (courses != null) subscription.setCurrentCourses(courses);
        if (instructors != null) subscription.setCurrentInstructors(instructors);
        if (storageGB != null) subscription.setCurrentStorageUsedGB(storageGB);

        subscriptionRepository.save(subscription);
    }

    // ============ HELPER METHODS ============

    private LocalDateTime calculateNextRenewalDate(TenantSubscription.BillingCycle cycle) {
        LocalDateTime now = LocalDateTime.now();
        return cycle == TenantSubscription.BillingCycle.MONTHLY
                ? now.plusMonths(1)
                : now.plusYears(1);
    }

    private SubscriptionPlanDTO convertPlanToDTO(SubscriptionPlan plan) {
        SubscriptionPlanDTO dto = new SubscriptionPlanDTO();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        dto.setPlanKey(plan.getPlanKey());
        dto.setDescription(plan.getDescription());
        dto.setMonthlyPrice(plan.getMonthlyPrice());
        dto.setAnnualPrice(plan.getAnnualPrice());
        dto.setCurrency(plan.getCurrency());
        dto.setMaxStudents(plan.getMaxStudents());
        dto.setMaxCourses(plan.getMaxCourses());
        dto.setMaxInstructors(plan.getMaxInstructors());
        dto.setMaxStorageGB(plan.getMaxStorageGB());
        dto.setCustomDomain(plan.getCustomDomain());
        dto.setCustomBranding(plan.getCustomBranding());
        dto.setAdvancedAnalytics(plan.getAdvancedAnalytics());
        dto.setApiAccess(plan.getApiAccess());
        dto.setSso(plan.getSso());
        dto.setSupport24x7(plan.getSupport24x7());
        dto.setDedicatedAccount(plan.getDedicatedAccount());
        dto.setAdditionalFeatures(plan.getAdditionalFeatures());
        dto.setDisplayOrder(plan.getDisplayOrder());
        dto.setIsRecommended(plan.getIsRecommended());
        dto.setStatus(plan.getStatus().toString());
        return dto;
    }

    private TenantSubscriptionDTO convertSubscriptionToDTO(TenantSubscription subscription) {
        TenantSubscriptionDTO dto = new TenantSubscriptionDTO();
        dto.setId(subscription.getId());
        dto.setTenantId(subscription.getTenant().getId());
        dto.setTenantName(subscription.getTenant().getName());
        dto.setPlanId(subscription.getPlan().getId());
        dto.setPlanName(subscription.getPlan().getName());
        dto.setBillingCycle(subscription.getBillingCycle().toString());
        dto.setCurrentPrice(subscription.getCurrentPrice());
        dto.setCurrency(subscription.getCurrency());
        dto.setStatus(subscription.getStatus().toString());
        dto.setIsTrialPeriod(subscription.getIsTrialPeriod());
        dto.setTrialDaysRemaining(subscription.getTrialDaysRemaining());
        dto.setSubscribedAt(subscription.getSubscribedAt());
        dto.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        dto.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        dto.setAutoRenew(subscription.getAutoRenew());
        dto.setCurrentStudents(subscription.getCurrentStudents());
        dto.setCurrentCourses(subscription.getCurrentCourses());
        dto.setCurrentInstructors(subscription.getCurrentInstructors());
        dto.setCurrentStorageUsedGB(subscription.getCurrentStorageUsedGB());
        dto.setMaxStudents(subscription.getPlan().getMaxStudents());
        dto.setMaxCourses(subscription.getPlan().getMaxCourses());
        dto.setMaxInstructors(subscription.getPlan().getMaxInstructors());
        dto.setMaxStorageGB(subscription.getPlan().getMaxStorageGB());
        return dto;
    }

    private void createAdminUser(Tenant tenant, TenantRegistrationRequest request) {
        User admin = new User();
        admin.setFirstName(request.getAdminFirstName());
        admin.setLastName(request.getAdminLastName());
        admin.setEmail(request.getAdminEmail());
        admin.setPassword(passwordEncoder.encode(request.getAdminPassword()));
        admin.setRole(User.Role.ADMIN);
        admin.setTenant(tenant);
        admin.setEmailVerified(true);
        admin.setStatus(User.Status.ACTIVE);

        userRepository.save(admin);
        log.info("Created admin user {} for tenant {}", request.getAdminEmail(), tenant.getSubdomain());
    }
}

