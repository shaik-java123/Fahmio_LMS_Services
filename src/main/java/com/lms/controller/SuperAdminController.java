package com.lms.controller;

import com.lms.model.Tenant;
import com.lms.repository.TenantRepository;
import com.lms.repository.UserRepository;
import com.lms.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTenants", tenantRepository.count());
        stats.put("activeTenants", tenantRepository.findByStatus(Tenant.Status.ACTIVE).size());
        stats.put("totalGlobalUsers", userRepository.count());
        stats.put("totalGlobalCourses", courseRepository.count());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<Tenant>> getAllTenants() {
        return ResponseEntity.ok(tenantRepository.findAll());
    }

    @PutMapping("/tenants/{id}/status")
    public ResponseEntity<Tenant> updateTenantStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        Tenant tenant = tenantRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        tenant.setStatus(Tenant.Status.valueOf(body.get("status").toUpperCase()));
        return ResponseEntity.ok(tenantRepository.save(tenant));
    }

    @PutMapping("/tenants/{id}/plan")
    public ResponseEntity<Tenant> updateTenantPlan(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        Tenant tenant = tenantRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        tenant.setPlan(Tenant.Plan.valueOf(body.get("plan").toUpperCase()));
        return ResponseEntity.ok(tenantRepository.save(tenant));
    }

    @PostMapping("/tenants")
    public ResponseEntity<Tenant> createTenant(@RequestBody Tenant tenant) {
        if (tenantRepository.existsBySubdomain(tenant.getSubdomain())) {
            throw new RuntimeException("Subdomain already exists");
        }
        tenant.setStatus(Tenant.Status.ACTIVE);
        return ResponseEntity.ok(tenantRepository.save(tenant));
    }
}
