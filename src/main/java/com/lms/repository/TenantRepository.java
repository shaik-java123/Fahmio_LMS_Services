package com.lms.repository;

import com.lms.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySubdomain(String subdomain);

    Optional<Tenant> findBySubdomainAndStatus(String subdomain, Tenant.Status status);

    Optional<Tenant> findByCustomDomainAndStatus(String customDomain, Tenant.Status status);

    boolean existsBySubdomain(String subdomain);

    boolean existsByCustomDomain(String customDomain);
    
    List<Tenant> findByStatus(Tenant.Status status);
}
