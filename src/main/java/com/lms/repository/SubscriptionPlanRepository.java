package com.lms.repository;

import com.lms.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByPlanKey(String planKey);

    Optional<SubscriptionPlan> findByName(String name);

    List<SubscriptionPlan> findByStatus(SubscriptionPlan.PlanStatus status);

    List<SubscriptionPlan> findByStatusOrderByDisplayOrder(SubscriptionPlan.PlanStatus status);

    List<SubscriptionPlan> findByIsRecommendedTrueOrderByDisplayOrder();

    boolean existsByPlanKey(String planKey);

    boolean existsByStripeProductId(String stripeProductId);
}

