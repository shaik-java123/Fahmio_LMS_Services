package com.lms.repository;

import com.lms.model.TenantSubscription;
import com.lms.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, Long> {

    Optional<TenantSubscription> findByTenant(Tenant tenant);

    Optional<TenantSubscription> findByTenantId(Long tenantId);

    Optional<TenantSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    List<TenantSubscription> findByStatus(TenantSubscription.SubscriptionStatus status);

    List<TenantSubscription> findByAutoRenewTrueAndCurrentPeriodEndBefore(LocalDateTime date);

    @Query("SELECT ts FROM TenantSubscription ts WHERE ts.status = 'ACTIVE' OR ts.status = 'TRIAL'")
    List<TenantSubscription> findAllActive();

    @Query("SELECT ts FROM TenantSubscription ts WHERE ts.currentPeriodEnd < :date AND ts.autoRenew = true AND (ts.status = 'ACTIVE' OR ts.status = 'TRIAL')")
    List<TenantSubscription> findExpiringSubscriptions(@Param("date") LocalDateTime date);

    @Query("SELECT ts FROM TenantSubscription ts WHERE ts.status = 'PAST_DUE' AND ts.currentPeriodEnd < :date")
    List<TenantSubscription> findOverdueSubscriptions(@Param("date") LocalDateTime date);
}

