package com.lms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private BillingCycle billingCycle = BillingCycle.MONTHLY; // MONTHLY or ANNUAL

    @Column(nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private BigDecimal currentPrice; // Price at time of subscription

    @Column(length = 3, columnDefinition = "CHAR(3) DEFAULT 'USD'")
    private String currency = "USD";

    // Stripe subscription tracking
    private String stripeSubscriptionId;
    private String stripeCustomerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    // Trial period
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isTrialPeriod = false;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer trialDaysRemaining = 0;

    // Dates
    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime subscribedAt;

    private LocalDateTime currentPeriodStart;

    private LocalDateTime currentPeriodEnd; // Next renewal date

    private LocalDateTime cancelledAt;

    private String cancellationReason;

    // Auto-renewal settings
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean autoRenew = true;

    // Usage tracking
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer currentStudents = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer currentCourses = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer currentInstructors = 0;

    @Column(name = "current_storage_used_gb", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long currentStorageUsedGB = 0L;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum BillingCycle {
        MONTHLY, ANNUAL
    }

    public enum SubscriptionStatus {
        ACTIVE,           // Currently active and billing
        TRIAL,            // In trial period
        PAUSED,           // Paused but not cancelled
        CANCELLED,        // Cancelled by user
        EXPIRED,          // Trial or payment period expired
        PAST_DUE,         // Payment failed
        SUSPENDED         // Admin suspended for non-payment or violation
    }

    // Helper methods
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIAL;
    }

    public boolean isPastDue() {
        return status == SubscriptionStatus.PAST_DUE;
    }

    public boolean canUpgrade() {
        return isActive() || status == SubscriptionStatus.PAUSED;
    }

    public boolean canDowngrade() {
        return isActive() || status == SubscriptionStatus.PAUSED;
    }

    public int getDaysUntilRenewal() {
        if (currentPeriodEnd == null) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(
            LocalDateTime.now(),
            currentPeriodEnd
        );
    }

    public boolean isRenewingSoon() {
        return getDaysUntilRenewal() <= 7 && getDaysUntilRenewal() > 0;
    }

    public boolean isStorageNearLimit() {
        if (plan.getMaxStorageGB() == 0) {
            return false; // Unlimited
        }
        double usage = (double) currentStorageUsedGB / plan.getMaxStorageGB();
        return usage >= 0.90; // 90% or more
    }

    public boolean canAddStudent() {
        if (plan.getMaxStudents() == 0) {
            return true; // Unlimited
        }
        return currentStudents < plan.getMaxStudents();
    }

    public boolean canAddCourse() {
        if (plan.getMaxCourses() == 0) {
            return true; // Unlimited
        }
        return currentCourses < plan.getMaxCourses();
    }

    public boolean canAddInstructor() {
        if (plan.getMaxInstructors() == 0) {
            return true; // Unlimited
        }
        return currentInstructors < plan.getMaxInstructors();
    }

    public String getStatusDisplayName() {
        return switch (status) {
            case ACTIVE -> "Active";
            case TRIAL -> "Trial";
            case PAUSED -> "Paused";
            case CANCELLED -> "Cancelled";
            case EXPIRED -> "Expired";
            case PAST_DUE -> "Payment Failed";
            case SUSPENDED -> "Suspended";
        };
    }
}

