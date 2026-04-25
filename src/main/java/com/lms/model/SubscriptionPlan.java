package com.lms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "subscription_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "Starter", "Growth", "Professional", "Enterprise"

    @Column(name = "plan_key", nullable = false, unique = true)
    private String planKey; // e.g., "starter", "growth", "professional", "enterprise"

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private BigDecimal monthlyPrice; // Monthly pricing

    @Column(nullable = false)
    private BigDecimal annualPrice; // Annual pricing (discounted)

    @Column(length = 3)
    private String currency = "USD";

    private String stripeProductId;
    private String stripePriceIdMonthly;
    private String stripePriceIdAnnual;

    // Feature limits
    @Column(nullable = false)
    private Integer maxStudents = 0; // 0 = unlimited

    @Column(nullable = false)
    private Integer maxCourses = 0; // 0 = unlimited

    @Column(nullable = false)
    private Integer maxInstructors = 0; // 0 = unlimited

    @Column(name = "max_storage_gb", nullable = false)
    private Long maxStorageGB = 0L; // 0 = unlimited

    @Column(nullable = false)
    private Boolean customDomain = false;

    @Column(nullable = false)
    private Boolean customBranding = false;

    @Column(nullable = false)
    private Boolean advancedAnalytics = false;

    @Column(nullable = false)
    private Boolean apiAccess = false;

    @Column(nullable = false)
    private Boolean sso = false; // Single Sign-On

    @Column(name = "support_24x7", nullable = false)
    private Boolean support24x7 = false;

    @Column(nullable = false)
    private Boolean dedicatedAccount = false;

    @ElementCollection
    @CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "feature")
    private Set<String> additionalFeatures = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private PlanStatus status = PlanStatus.ACTIVE;

    @Column(nullable = false)
    private Integer displayOrder = 0; // Order in which plans are displayed

    @Column(nullable = false)
    private Boolean isRecommended = false; // Highlight as recommended plan

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum PlanStatus {
        ACTIVE, ARCHIVED, BETA
    }

    // Convenience methods for pricing
    public BigDecimal getMonthlyPrice() {
        return monthlyPrice != null ? monthlyPrice : BigDecimal.ZERO;
    }

    public BigDecimal getAnnualPrice() {
        return annualPrice != null ? annualPrice : BigDecimal.ZERO;
    }

    public BigDecimal getMonthlyEquivalentPrice() {
        // Calculate monthly equivalent of annual price
        if (annualPrice != null && annualPrice.compareTo(BigDecimal.ZERO) > 0) {
            return annualPrice.divide(new BigDecimal(12), 2, java.math.RoundingMode.HALF_UP);
        }
        return monthlyPrice != null ? monthlyPrice : BigDecimal.ZERO;
    }

    public BigDecimal getAnnualDiscount() {
        // Calculate discount percentage when paying annually
        if (monthlyPrice != null && monthlyPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal annualMonthlyEquivalent = monthlyPrice.multiply(new BigDecimal(12));
            if (annualPrice != null && annualPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal savings = annualMonthlyEquivalent.subtract(annualPrice);
                return savings.divide(annualMonthlyEquivalent, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100));
            }
        }
        return BigDecimal.ZERO;
    }
}

