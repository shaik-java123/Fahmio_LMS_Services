package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantRegistrationRequest {
    private String name;
    private String subdomain;
    private String contactEmail;
    private String customDomain;
    private String logoUrl;
    private String primaryColor;
    private String accentColor;
    private String tagline;

    // Subscription details
    private String planKey; // e.g., "starter", "growth", "business", "enterprise"
    private String billingCycle; // "MONTHLY" or "ANNUAL"

    // Admin user for this tenant
    private String adminFirstName;
    private String adminLastName;
    private String adminEmail;
    private String adminPassword;
}

