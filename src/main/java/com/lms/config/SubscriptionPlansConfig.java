package com.lms.config;

import com.lms.model.SubscriptionPlan;
import com.lms.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlansConfig {

    private final SubscriptionPlanRepository planRepository;

    @Bean
    public CommandLineRunner setupSubscriptionPlans() {
        return new CommandLineRunner() {
            @Override
            @Transactional
            public void run(String... args) throws Exception {
                log.info("Starting subscription plans initialization...");

                // Check if plans already exist
                if (planRepository.count() > 0) {
                    log.info("Subscription plans already exist. Skipping initialization.");
                    return;
                }

                // Create Starter Plan
                SubscriptionPlan freePlan = new SubscriptionPlan();
                freePlan.setName("Starter");
                freePlan.setPlanKey("starter");
                freePlan.setDescription("Perfect for getting started with online learning");
                freePlan.setMonthlyPrice(BigDecimal.ZERO);
                freePlan.setAnnualPrice(BigDecimal.ZERO);
                freePlan.setCurrency("USD");
                freePlan.setMaxStudents(50);
                freePlan.setMaxCourses(5);
                freePlan.setMaxInstructors(2);
                freePlan.setMaxStorageGB(5L);
                freePlan.setCustomDomain(false);
                freePlan.setCustomBranding(false);
                freePlan.setAdvancedAnalytics(false);
                freePlan.setApiAccess(false);
                freePlan.setSso(false);
                freePlan.setSupport24x7(false);
                freePlan.setDedicatedAccount(false);
                freePlan.setStatus(SubscriptionPlan.PlanStatus.ACTIVE);
                freePlan.setDisplayOrder(0);
                freePlan.setIsRecommended(false);
                planRepository.save(freePlan);
                log.info("✅ Starter plan created");

                // Create Growth Plan
                SubscriptionPlan basicPlan = new SubscriptionPlan();
                basicPlan.setName("Growth");
                basicPlan.setPlanKey("growth");
                basicPlan.setDescription("Great for small teams and growing organizations");
                basicPlan.setMonthlyPrice(new BigDecimal("29.99"));
                basicPlan.setAnnualPrice(new BigDecimal("299.90"));
                basicPlan.setCurrency("USD");
                basicPlan.setMaxStudents(500);
                basicPlan.setMaxCourses(25);
                basicPlan.setMaxInstructors(10);
                basicPlan.setMaxStorageGB(50L);
                basicPlan.setCustomDomain(true);
                basicPlan.setCustomBranding(true);
                basicPlan.setAdvancedAnalytics(false);
                basicPlan.setApiAccess(false);
                basicPlan.setSso(false);
                basicPlan.setSupport24x7(false);
                basicPlan.setDedicatedAccount(false);
                basicPlan.setStatus(SubscriptionPlan.PlanStatus.ACTIVE);
                basicPlan.setDisplayOrder(1);
                basicPlan.setIsRecommended(false);
                planRepository.save(basicPlan);
                log.info("✅ Growth plan created");

                // Create Business Plan
                SubscriptionPlan businessPlan = new SubscriptionPlan();
                businessPlan.setName("Business");
                businessPlan.setPlanKey("business");
                businessPlan.setDescription("For established organizations with advanced needs");
                businessPlan.setMonthlyPrice(new BigDecimal("99.99"));
                businessPlan.setAnnualPrice(new BigDecimal("999.90"));
                businessPlan.setCurrency("USD");
                businessPlan.setMaxStudents(5000);
                businessPlan.setMaxCourses(100);
                businessPlan.setMaxInstructors(50);
                businessPlan.setMaxStorageGB(500L);
                businessPlan.setCustomDomain(true);
                businessPlan.setCustomBranding(true);
                businessPlan.setAdvancedAnalytics(true);
                businessPlan.setApiAccess(true);
                businessPlan.setSso(false);
                businessPlan.setSupport24x7(false);
                businessPlan.setDedicatedAccount(false);
                businessPlan.setStatus(SubscriptionPlan.PlanStatus.ACTIVE);
                businessPlan.setDisplayOrder(2);
                businessPlan.setIsRecommended(true);
                planRepository.save(businessPlan);
                log.info("✅ Business plan created");

                // Create Enterprise Plan
                SubscriptionPlan proPlan = new SubscriptionPlan();
                proPlan.setName("Enterprise");
                proPlan.setPlanKey("enterprise");
                proPlan.setDescription("Unlimited scale with premium support and dedicated management");
                proPlan.setMonthlyPrice(new BigDecimal("299.99"));
                proPlan.setAnnualPrice(new BigDecimal("2999.90"));
                proPlan.setCurrency("USD");
                proPlan.setMaxStudents(0); // Unlimited
                proPlan.setMaxCourses(0); // Unlimited
                proPlan.setMaxInstructors(0); // Unlimited
                proPlan.setMaxStorageGB(5000L);
                proPlan.setCustomDomain(true);
                proPlan.setCustomBranding(true);
                proPlan.setAdvancedAnalytics(true);
                proPlan.setApiAccess(true);
                proPlan.setSso(true);
                proPlan.setSupport24x7(true);
                proPlan.setDedicatedAccount(true);
                proPlan.setStatus(SubscriptionPlan.PlanStatus.ACTIVE);
                proPlan.setDisplayOrder(3);
                proPlan.setIsRecommended(false);
                planRepository.save(proPlan);
                log.info("✅ Enterprise plan created");

                System.out.println("=========================================================");
                System.out.println("✅ All 4 subscription plans initialized successfully!");
                System.out.println("   - Starter (Free)");
                System.out.println("   - Growth ($29.99/month)");
                System.out.println("   - Business ($99.99/month) - RECOMMENDED");
                System.out.println("   - Enterprise ($299.99/month)");
                System.out.println("=========================================================");
            }
        };
    }
}
