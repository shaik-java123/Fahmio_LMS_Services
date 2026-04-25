package com.lms.service;

import com.lms.dto.CoursePerformanceDTO;
import com.lms.dto.InstructorAnalyticsDTO;
import com.lms.model.Course;
import com.lms.model.Order;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.OrderRepository;
import com.lms.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.lms.dto.AdminAnalyticsDTO;
import com.lms.model.Enrollment;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final com.lms.repository.LessonRepository lessonRepository;
    private final com.lms.repository.LessonProgressRepository lpRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCourseDropOffAnalytics(Long courseId) {
        List<com.lms.model.Lesson> allLessons = lessonRepository.findAllInCourseOrderByOrder(courseId);
        long totalEnrolled = enrollmentRepository.countByCourseId(courseId);

        return allLessons.stream().map(lesson -> {
            long started = lpRepository.countByLessonId(lesson.getId());
            long completed = lpRepository.countByLessonIdAndCompletedTrue(lesson.getId());
            
            Map<String, Object> data = new HashMap<>();
            data.put("lessonId", lesson.getId());
            data.put("title", lesson.getTitle());
            data.put("completionRate", started > 0 ? (double) completed / started : 1.0);
            data.put("dropoffCount", started - completed);
            data.put("engagementPct", totalEnrolled > 0 ? (double) started / totalEnrolled : 0.0);
            return data;
        }).collect(Collectors.toList());
    }
    public AdminAnalyticsDTO getAdminRevenueAnalytics() {
        String subdomain = com.lms.tenant.TenantContext.getCurrentTenant();
        boolean isGlobal = subdomain.equals("global") || subdomain.equals("localhost");

        List<Order> allCompletedOrders = orderRepository.findAllByStatus(Order.OrderStatus.COMPLETED).stream()
                .filter(o -> isGlobal || (o.getCourse() != null && o.getCourse().getTenant() != null && o.getCourse().getTenant().getSubdomain().equals(subdomain)))
                .collect(Collectors.toList());
        
        Double totalRevenue = allCompletedOrders.stream()
                .mapToDouble(o -> o.getAmount().doubleValue())
                .sum();

        List<Enrollment> activeEnrollments = enrollmentRepository.findAllByStatus(Enrollment.Status.ACTIVE).stream()
                .filter(e -> isGlobal || (e.getCourse().getTenant() != null && e.getCourse().getTenant().getSubdomain().equals(subdomain)))
                .collect(Collectors.toList());

        Long activeSubsCount = activeEnrollments.stream()
                .filter(e -> e.getStripeSubscriptionId() != null)
                .count();

        // Calculate MRR: For each active subscription, find the course price
        Double mrr = activeEnrollments.stream()
                .filter(e -> e.getStripeSubscriptionId() != null)
                .mapToDouble(e -> e.getCourse().getPrice() != null ? e.getCourse().getPrice().doubleValue() : 0.0)
                .sum();

        Long expiredSubs = enrollmentRepository.findAllByStatus(Enrollment.Status.EXPIRED).stream()
                .filter(e -> isGlobal || (e.getCourse().getTenant() != null && e.getCourse().getTenant().getSubdomain().equals(subdomain)))
                .count();

        // Revenue trend
        Map<String, Double> revenueTrend = allCompletedOrders.stream()
                .collect(Collectors.groupingBy(
                    o -> o.getCreatedAt() != null 
                        ? o.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM yyyy"))
                        : "Unknown",
                    TreeMap::new,
                    Collectors.summingDouble(o -> o.getAmount().doubleValue())
                ));

        // Recent sales - filtered by tenant
        List<AdminAnalyticsDTO.RecentSaleDTO> recentSales = allCompletedOrders.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10)
                .map(o -> AdminAnalyticsDTO.RecentSaleDTO.builder()
                        .studentName(o.getUser().getFirstName() + " " + o.getUser().getLastName())
                        .courseTitle(o.getCourse() != null ? o.getCourse().getTitle() : "Multiple Items")
                        .amount(o.getAmount().doubleValue())
                        .date(o.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                        .status(o.getStatus().name())
                        .type(o.getStripeSessionId() != null ? "STRIPE" : (o.getRazorpayOrderId() != null ? "RAZORPAY" : "LOCAL"))
                        .build())
                .collect(Collectors.toList());

        return AdminAnalyticsDTO.builder()
                .totalRevenue(totalRevenue)
                .activeSubscriptions(activeSubsCount)
                .mrr(mrr)
                .totalOrders((long) allCompletedOrders.size())
                .expiredSubscriptions(expiredSubs)
                .revenueTrend(revenueTrend)
                .recentSales(recentSales)
                .build();
    }

    @Transactional(readOnly = true)
    public InstructorAnalyticsDTO getInstructorAnalytics(Long instructorId) {
        List<Course> instructorCourses = courseRepository.findByInstructorId(instructorId);
        
        if (instructorCourses.isEmpty()) {
            return InstructorAnalyticsDTO.builder()
                .totalRevenue(0.0)
                .totalStudents(0L)
                .totalCourses(0L)
                .totalEnrollments(0L)
                .coursePerformance(Collections.emptyList())
                .monthlyRevenue(Collections.emptyMap())
                .build();
        }

        Set<Long> courseIds = instructorCourses.stream().map(Course::getId).collect(Collectors.toSet());
        
        // Revenue logic
        List<Order> instructorOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                .filter(o -> o.getItems().stream().anyMatch(item -> courseIds.contains(item.getCourse().getId())))
                .collect(Collectors.toList());
        
        Double totalRevenue = instructorOrders.stream()
                .mapToDouble(o -> o.getAmount().doubleValue())
                .sum(); // Unique student count across all courses
        Long totalEnrollments = enrollmentRepository.findAll().stream()
                .filter(e -> courseIds.contains(e.getCourse().getId()))
                .count();

        Set<Long> uniqueStudentIds = enrollmentRepository.findAll().stream()
                .filter(e -> courseIds.contains(e.getCourse().getId()))
                .map(e -> e.getStudent().getId())
                .collect(Collectors.toSet());
        
        // Course performance list
        List<CoursePerformanceDTO> performance = instructorCourses.stream().map(c -> {
            Long students = enrollmentRepository.countByCourseId(c.getId());
            Double revenue = instructorOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .filter(item -> item.getCourse().getId().equals(c.getId()))
                .mapToDouble(item -> item.getAmount())
                .sum();
            Double avgRating = reviewRepository.getAverageRatingForCourse(c.getId());

            return CoursePerformanceDTO.builder()
                .courseId(c.getId())
                .title(c.getTitle())
                .studentCount(students)
                .revenue(revenue)
                .averageRating(avgRating != null ? avgRating : 0.0)
                .build();
        }).collect(Collectors.toList());

        // Monthly revenue trend (simple version)
        Map<String, Double> monthlyTrend = instructorOrders.stream()
                .collect(Collectors.groupingBy(
                    o -> o.getCreatedAt() != null 
                        ? o.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM yyyy"))
                        : "Unknown Date",
                    TreeMap::new,
                    Collectors.summingDouble(o -> o.getAmount().doubleValue())
                ));

        return InstructorAnalyticsDTO.builder()
                .totalRevenue(totalRevenue)
                .totalStudents((long) uniqueStudentIds.size())
                .totalCourses((long) instructorCourses.size())
                .totalEnrollments(totalEnrollments)
                .coursePerformance(performance)
                .monthlyRevenue(monthlyTrend)
                .build();
    }
}
