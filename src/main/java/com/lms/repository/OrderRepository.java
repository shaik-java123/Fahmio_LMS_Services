package com.lms.repository;

import com.lms.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByStripeSessionId(String stripeSessionId);
    Optional<Order> findByStripePaymentIntentId(String stripePaymentIntentId);
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);
    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, Order.OrderStatus status);
    
    List<Order> findAllByStatus(Order.OrderStatus status);
    
    List<Order> findTop10ByStatusOrderByCreatedAtDesc(Order.OrderStatus status);
}
