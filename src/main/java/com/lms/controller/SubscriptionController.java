package com.lms.controller;

import com.lms.dto.SubscriptionPlanDTO;
import com.lms.dto.TenantRegistrationRequest;
import com.lms.dto.TenantSubscriptionDTO;
import com.lms.model.SubscriptionPlan;
import com.lms.model.TenantSubscription;
import com.lms.repository.TenantRepository;
import com.lms.service.SubscriptionService;
import com.lms.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subscriptions", description = "Subscription plans and tenant subscription management")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final TenantRepository tenantRepository;

    // ============ PUBLIC ENDPOINTS (No Auth Required) ============

    /**
     * Get all available subscription plans
     * Used on pricing page and registration page
     */
    @GetMapping("/plans")
    @Operation(summary = "Get all available subscription plans")
    public ResponseEntity<List<SubscriptionPlanDTO>> getAllPlans() {
        log.debug("Fetching all available subscription plans");
        List<SubscriptionPlanDTO> plans = subscriptionService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    /**
     * Register new tenant with subscription
     * This is the main endpoint for onboarding new organizations
     */
    @PostMapping("/register-tenant")
    @Operation(summary = "Register a new tenant with subscription plan")
    public ResponseEntity<TenantSubscriptionDTO> registerTenant(
            @RequestBody TenantRegistrationRequest request) {
        log.info("Registering new tenant: {}", request.getSubdomain());
        try {
            TenantSubscriptionDTO subscription = subscriptionService.registerTenantWithSubscription(request);
            return ResponseEntity.ok(subscription);
        } catch (Exception e) {
            log.error("Error registering tenant: {}", e.getMessage());
            throw e;
        }
    }

    // ============ AUTHENTICATED ENDPOINTS ============

    /**
     * Get current tenant's subscription details
     */
    @GetMapping("/my-subscription")
    @Operation(summary = "Get current tenant's subscription details")
    public ResponseEntity<TenantSubscriptionDTO> getMySubscription() {
        // In a real app, get tenantId from context/JWT
        // For now, assuming it's passed or retrieved from auth
        Long tenantId = getTenantIdFromContext();
        TenantSubscriptionDTO subscription = subscriptionService.getTenantSubscription(tenantId);
        return ResponseEntity.ok(subscription);
    }

    /**
     * Change tenant's subscription plan (upgrade/downgrade)
     */
    @PostMapping("/change-plan")
    @Operation(summary = "Change subscription plan (upgrade or downgrade)")
    public ResponseEntity<TenantSubscriptionDTO> changePlan(
            @RequestBody Map<String, String> request) {
        Long tenantId = getTenantIdFromContext();
        String planKey = request.get("planKey");
        String billingCycle = request.getOrDefault("billingCycle", "MONTHLY");

        log.info("Changing plan for tenant {} to {}", tenantId, planKey);
        TenantSubscription subscription = subscriptionService.changePlan(tenantId, planKey, billingCycle);

        TenantSubscriptionDTO dto = convertToDTO(subscription);
        return ResponseEntity.ok(dto);
    }

    /**
     * Cancel subscription
     */
    @PostMapping("/cancel")
    @Operation(summary = "Cancel current subscription")
    public ResponseEntity<Map<String, String>> cancelSubscription(
            @RequestBody Map<String, String> request) {
        Long tenantId = getTenantIdFromContext();
        String reason = request.getOrDefault("reason", "User requested");

        log.info("Cancelling subscription for tenant {}", tenantId);
        subscriptionService.cancelSubscription(tenantId, reason);

        return ResponseEntity.ok(Map.of("message", "Subscription cancelled successfully"));
    }

    /**
     * Check if tenant can add student
     */
    @GetMapping("/can-add-student")
    @Operation(summary = "Check if tenant can add more students based on plan")
    public ResponseEntity<Map<String, Boolean>> canAddStudent() {
        Long tenantId = getTenantIdFromContext();
        boolean canAdd = subscriptionService.canAddStudent(tenantId);
        return ResponseEntity.ok(Map.of("canAdd", canAdd));
    }

    /**
     * Check if tenant can add course
     */
    @GetMapping("/can-add-course")
    @Operation(summary = "Check if tenant can add more courses based on plan")
    public ResponseEntity<Map<String, Boolean>> canAddCourse() {
        Long tenantId = getTenantIdFromContext();
        boolean canAdd = subscriptionService.canAddCourse(tenantId);
        return ResponseEntity.ok(Map.of("canAdd", canAdd));
    }

    /**
     * Check if tenant can add instructor
     */
    @GetMapping("/can-add-instructor")
    @Operation(summary = "Check if tenant can add more instructors based on plan")
    public ResponseEntity<Map<String, Boolean>> canAddInstructor() {
        Long tenantId = getTenantIdFromContext();
        boolean canAdd = subscriptionService.canAddInstructor(tenantId);
        return ResponseEntity.ok(Map.of("canAdd", canAdd));
    }

    /**
     * Update usage metrics for tenant
     */
    @PutMapping("/usage-metrics")
    @Operation(summary = "Update current usage metrics")
    public ResponseEntity<Map<String, String>> updateUsageMetrics(
            @RequestBody Map<String, Integer> metrics) {
        Long tenantId = getTenantIdFromContext();

        Integer students = metrics.get("students");
        Integer courses = metrics.get("courses");
        Integer instructors = metrics.get("instructors");
        Long storageGB = metrics.get("storageGB") != null ? metrics.get("storageGB").longValue() : null;

        subscriptionService.updateUsageMetrics(tenantId, students, courses, instructors, storageGB);

        return ResponseEntity.ok(Map.of("message", "Usage metrics updated"));
    }

    // ============ ADMIN ENDPOINTS ============

    /**
     * Create new subscription plan (SUPER_ADMIN only)
     */
    @PostMapping("/plans")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create new subscription plan")
    public ResponseEntity<SubscriptionPlan> createPlan(@RequestBody SubscriptionPlan plan) {
        log.info("Creating new subscription plan: {}", plan.getPlanKey());
        SubscriptionPlan created = subscriptionService.createPlan(plan);
        return ResponseEntity.ok(created);
    }

    /**
     * Update subscription plan
     */
    @PutMapping("/plans/{planId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update subscription plan")
    public ResponseEntity<SubscriptionPlan> updatePlan(
            @PathVariable Long planId,
            @RequestBody SubscriptionPlan updates) {
        log.info("Updating subscription plan: {}", planId);
        SubscriptionPlan updated = subscriptionService.updatePlan(planId, updates);
        return ResponseEntity.ok(updated);
    }

    /**
     * Subscribe tenant to plan (SUPER_ADMIN only)
     */
    @PostMapping("/subscribe")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Subscribe tenant to a plan")
    public ResponseEntity<TenantSubscriptionDTO> subscribeTenant(
            @RequestBody Map<String, String> request) {
        Long tenantId = Long.parseLong(request.get("tenantId"));
        String planKey = request.get("planKey");
        String billingCycle = request.getOrDefault("billingCycle", "MONTHLY");

        log.info("Subscribing tenant {} to plan {}", tenantId, planKey);
        TenantSubscription subscription = subscriptionService.subscribeToPlan(tenantId, planKey, billingCycle);

        TenantSubscriptionDTO dto = convertToDTO(subscription);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get subscription for specific tenant
     */
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    @Operation(summary = "Get subscription details for specific tenant")
    public ResponseEntity<TenantSubscriptionDTO> getTenantSubscription(
            @PathVariable Long tenantId) {
        log.debug("Fetching subscription for tenant: {}", tenantId);
        TenantSubscriptionDTO subscription = subscriptionService.getTenantSubscription(tenantId);
        return ResponseEntity.ok(subscription);
    }

    // ============ HELPER METHODS ============

    private Long getTenantIdFromContext() {
        String subdomain = TenantContext.getCurrentTenant();
        if (subdomain == null || subdomain.isBlank()) {
            throw new RuntimeException("No tenant context found");
        }
        return tenantRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + subdomain))
                .getId();
    }

    private TenantSubscriptionDTO convertToDTO(TenantSubscription subscription) {
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
        dto.setAutoRenew(subscription.getAutoRenew());
        dto.setCurrentStudents(subscription.getCurrentStudents());
        dto.setCurrentCourses(subscription.getCurrentCourses());
        dto.setCurrentInstructors(subscription.getCurrentInstructors());
        dto.setCurrentStorageUsedGB(subscription.getCurrentStorageUsedGB());
        return dto;
    }
}

