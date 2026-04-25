package com.lms.controller;

import com.lms.model.Tenant;
import com.lms.service.TenantService;
import com.lms.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /** Public: resolve tenant branding from subdomain (called by frontend on boot) */
    @GetMapping("/resolve")
    public ResponseEntity<Tenant> resolveTenant(@RequestParam(required = false) String subdomain) {
        if (subdomain != null) {
            return ResponseEntity.ok(tenantService.resolveTenant(subdomain));
        }
        return ResponseEntity.ok(tenantService.getCurrentTenant());
    }

    /** Public: get current tenant info from request context */
    @GetMapping("/current")
    public ResponseEntity<Tenant> getCurrentTenant() {
        return ResponseEntity.ok(tenantService.getCurrentTenant());
    }

    /** Admin: create a new tenant */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tenant> createTenant(@RequestBody Tenant tenant) {
        return ResponseEntity.ok(tenantService.createTenant(tenant));
    }

    /** Admin: update branding */
    @PutMapping("/{id}/branding")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tenant> updateBranding(@PathVariable Long id,
                                                  @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(tenantService.updateBranding(
                id,
                body.get("logoUrl"),
                body.get("primaryColor"),
                body.get("accentColor"),
                body.get("tagline")));
    }

    /** Admin: set custom domain */
    @PutMapping("/{id}/custom-domain")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tenant> setCustomDomain(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(tenantService.setCustomDomain(id, body.get("customDomain")));
    }

    /** Admin: get current tenant for settings page */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Tenant> getMyTenant() {
        return ResponseEntity.ok(tenantService.getCurrentTenant());
    }

    /** Admin: update current tenant settings */
    @PutMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Tenant> updateMyTenant(@RequestBody Tenant tenant) {
        // Ensure user is updating their OWN tenant (current context)
        Tenant current = tenantService.getCurrentTenant();
        tenant.setId(current.getId());
        return ResponseEntity.ok(tenantService.updateTenant(tenant));
    }

    /** Super-admin: list all tenants */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Tenant>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    /** Super-admin: suspend tenant */
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> suspendTenant(@PathVariable Long id) {
        tenantService.suspendTenant(id);
        return ResponseEntity.ok(Map.of("message", "Tenant suspended"));
    }
}
