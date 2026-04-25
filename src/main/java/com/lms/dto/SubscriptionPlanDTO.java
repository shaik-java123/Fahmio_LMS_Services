package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanDTO {
    private Long id;
    private String name;
    private String planKey;
    private String description;
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
    private String currency;

    private Integer maxStudents;
    private Integer maxCourses;
    private Integer maxInstructors;
    private Long maxStorageGB;

    private Boolean customDomain;
    private Boolean customBranding;
    private Boolean advancedAnalytics;
    private Boolean apiAccess;
    private Boolean sso;
    private Boolean support24x7;
    private Boolean dedicatedAccount;

    private Set<String> additionalFeatures;
    private Integer displayOrder;
    private Boolean isRecommended;
    private String status;
}

