package com.lms.tenant;

import com.lms.model.Tenant;
import com.lms.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Skip tenant resolution for auth/swagger endpoints (works with any context-path)
            String requestUri = request.getRequestURI();
            if (isSkippedPath(requestUri)) {
                TenantContext.setCurrentTenant("global");
                filterChain.doFilter(request, response);
                return;
            }

            try {
                String tenantId = resolveTenant(request);
                if (tenantId != null) {
                    TenantContext.setCurrentTenant(tenantId);
                    log.debug("Resolved tenant: {}", tenantId);
                } else {
                    // Default to "global" tenant
                    TenantContext.setCurrentTenant("global");
                }
            } catch (Exception e) {
                // Database may not be connected yet
                log.warn("Failed to resolve tenant, using default: {}", e.getMessage());
                TenantContext.setCurrentTenant("global");
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Returns true for paths that bypass tenant resolution and always use global context.
     * - swagger/actuator: infrastructure paths
     * - super-admin: platform-wide, never tenant-scoped
     */
    private boolean isSkippedPath(String uri) {
        return uri.contains("/swagger-ui")
            || uri.contains("/v3/api-docs")
            || uri.contains("/actuator/health")
            || uri.contains("/super-admin");
    }

    private String resolveTenant(HttpServletRequest request) {
        // 1. Try explicit header (for API clients)
        String tenantHeader = request.getHeader("X-Tenant-ID");
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            return tenantHeader;
        }

        // 2. Try subdomain resolution
        String host = request.getServerName(); // e.g. acme.learnhub.io
        if (host != null && host.contains(".")) {
            String subdomain = host.split("\\.")[0];
            if (!subdomain.equals("www") && !subdomain.equals("api")) {
                Optional<Tenant> tenant = tenantRepository.findBySubdomainAndStatus(
                        subdomain, Tenant.Status.ACTIVE);
                if (tenant.isPresent()) {
                    return subdomain;
                }
            }
        }

        // 3. Check custom domain
        if (host != null) {
            Optional<Tenant> tenant = tenantRepository.findByCustomDomainAndStatus(
                    host, Tenant.Status.ACTIVE);
            if (tenant.isPresent()) {
                return tenant.get().getSubdomain();
            }
        }

        return null;
    }
}
