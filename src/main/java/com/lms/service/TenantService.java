package com.lms.service;

import com.lms.model.Tenant;
import com.lms.repository.TenantRepository;
import com.lms.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public Tenant resolveTenant(String subdomain) {
        return tenantRepository.findBySubdomainAndStatus(subdomain, Tenant.Status.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + subdomain));
    }

    @Transactional
    public Tenant getCurrentTenant() {
        String subdomain = TenantContext.getCurrentTenant();
        // Fallback to "global" if context is empty or specifically "global"
        if (subdomain == null || subdomain.equals("default") || subdomain.equals("global")) {
            return tenantRepository.findBySubdomain("global")
                    .orElseGet(() -> {
                        Tenant demo = new Tenant();
                        demo.setName("Platform Global");
                        demo.setSubdomain("global");
                        demo.setTagline("LMS Infrastructure");
                        demo.setPrimaryColor("#4f46e5");
                        demo.setAccentColor("#db2777");
                        demo.setPlan(Tenant.Plan.BUSINESS);
                        demo.setStatus(Tenant.Status.ACTIVE);
                        return tenantRepository.save(demo);
                    });
        }
        return resolveTenant(subdomain);
    }

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        if (tenantRepository.existsBySubdomain(tenant.getSubdomain())) {
            throw new RuntimeException("Subdomain already taken: " + tenant.getSubdomain());
        }
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant updateBranding(Long tenantId, String logoUrl, String primaryColor,
                                  String accentColor, String tagline) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        if (logoUrl != null) tenant.setLogoUrl(logoUrl);
        if (primaryColor != null) tenant.setPrimaryColor(primaryColor);
        if (accentColor != null) tenant.setAccentColor(accentColor);
        if (tagline != null) tenant.setTagline(tagline);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant setCustomDomain(Long tenantId, String customDomain) {
        if (tenantRepository.existsByCustomDomain(customDomain)) {
            throw new RuntimeException("Custom domain already registered");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        tenant.setCustomDomain(customDomain);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void suspendTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        tenant.setStatus(Tenant.Status.SUSPENDED);
        tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant updateTenant(Tenant updated) {
        Tenant existing = tenantRepository.findById(updated.getId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        // Update general info
        existing.setName(updated.getName());
        existing.setLogoUrl(updated.getLogoUrl());
        existing.setPrimaryColor(updated.getPrimaryColor());
        existing.setAccentColor(updated.getAccentColor());
        existing.setTagline(updated.getTagline());
        existing.setContactEmail(updated.getContactEmail());

        // Update certificate configuration
        existing.setCertSignatureUrl(updated.getCertSignatureUrl());
        existing.setCertAuthorityName(updated.getCertAuthorityName());
        existing.setCertAuthorityTitle(updated.getCertAuthorityTitle());
        existing.setCertBackgroundImageUrl(updated.getCertBackgroundImageUrl());
        existing.setCertLogoUrl(updated.getCertLogoUrl());
        existing.setCertPrimaryColor(updated.getCertPrimaryColor());
        existing.setCertAccentColor(updated.getCertAccentColor());
        existing.setCertFooterText(updated.getCertFooterText());

        return tenantRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }
}
