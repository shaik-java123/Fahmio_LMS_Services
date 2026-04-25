package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantSubscriptionDTO {
    private Long id;
    private Long tenantId;
    private String tenantName;
    private Long planId;
    private String planName;
    private String billingCycle;
    private BigDecimal currentPrice;
    private String currency;
    private String status;
    private Boolean isTrialPeriod;
    private Integer trialDaysRemaining;
    private LocalDateTime subscribedAt;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private Boolean autoRenew;
    private Integer currentStudents;
    private Integer currentCourses;
    private Integer currentInstructors;
    private Long currentStorageUsedGB;
    private Integer maxStudents;
    private Integer maxCourses;
    private Integer maxInstructors;
    private Long maxStorageGB;
}

