package com.lms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String subdomain;

    @Column(unique = true)
    private String customDomain;

    private String logoUrl;

    private String primaryColor = "#0284c7";

    private String accentColor = "#0ea5e9";

    @Column(length = 500)
    private String tagline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private Plan plan = Plan.STARTER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private Status status = Status.ACTIVE;

    private String contactEmail;
    private String stripeAccountId;

    // Certificate Configuration
    private String certSignatureUrl;
    private String certAuthorityName;
    private String certAuthorityTitle;
    private String certBackgroundImageUrl;
    private String certLogoUrl;
    private String certPrimaryColor = "#4f46e5"; // Indigo-600 default
    private String certAccentColor = "#db2777";  // Pink-600 default
    private String certFooterText;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Plan {
        STARTER, GROWTH, BUSINESS, ENTERPRISE
    }

    public enum Status {
        ACTIVE, SUSPENDED, DELETED
    }
}
