package com.lms.controller;

import com.lms.model.Course;
import com.lms.model.User;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final com.lms.service.AnalyticsService analyticsService;

    @GetMapping("/revenue")
    public ResponseEntity<com.lms.dto.AdminAnalyticsDTO> getRevenue() {
        return ResponseEntity.ok(analyticsService.getAdminRevenueAnalytics());
    }

    /** Platform-wide analytics summary filtered by tenant */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        String subdomain = com.lms.tenant.TenantContext.getCurrentTenant();
        Map<String, Object> stats = new HashMap<>();

        if (subdomain.equals("global") || subdomain.equals("localhost")) {
            stats.put("totalUsers", userRepository.count());
            stats.put("totalCourses", courseRepository.count());
            stats.put("totalEnrollments", enrollmentRepository.count());
            stats.put("publishedCourses", courseRepository.countByPublishedTrue());
            stats.put("totalStudents", userRepository.findByRole(User.Role.STUDENT).size());
            stats.put("totalInstructors", userRepository.findByRole(User.Role.INSTRUCTOR).size());
        } else {
            stats.put("totalUsers", userRepository.findByRoleAndTenantSubdomain(User.Role.STUDENT, subdomain).size() +
                                   userRepository.findByRoleAndTenantSubdomain(User.Role.INSTRUCTOR, subdomain).size());
            stats.put("totalCourses", courseRepository.findByTenantSubdomain(subdomain).size());
            stats.put("totalEnrollments", enrollmentRepository.findAll().stream()
                    .filter(e -> e.getCourse().getTenant() != null && e.getCourse().getTenant().getSubdomain().equals(subdomain))
                    .count());
            stats.put("publishedCourses", courseRepository.findByPublishedTrueAndTenantSubdomain(subdomain).size());
            stats.put("totalStudents", userRepository.findByRoleAndTenantSubdomain(User.Role.STUDENT, subdomain).size());
            stats.put("totalInstructors", userRepository.findByRoleAndTenantSubdomain(User.Role.INSTRUCTOR, subdomain).size());
        }

        return ResponseEntity.ok(stats);
    }

    /** List all users for current tenant */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        String subdomain = com.lms.tenant.TenantContext.getCurrentTenant();
        if (subdomain.equals("global") || subdomain.equals("localhost")) {
            return ResponseEntity.ok(userRepository.findAll());
        }
        return ResponseEntity.ok(userRepository.findByTenantSubdomain(subdomain));
    }

    /** Toggle user enabled/disabled */
    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(@PathVariable("id") Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        Map<String, Object> res = new HashMap<>();
        res.put("id", user.getId());
        res.put("enabled", user.isEnabled());
        return ResponseEntity.ok(res);
    }

    /** Change user role */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> changeUserRole(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(User.Role.valueOf(body.get("role")));
        userRepository.save(user);
        Map<String, Object> res = new HashMap<>();
        res.put("id", user.getId());
        res.put("role", user.getRole());
        return ResponseEntity.ok(res);
    }

    /** List ALL courses for current tenant */
    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getAllCourses() {
        String subdomain = com.lms.tenant.TenantContext.getCurrentTenant();
        if (subdomain.equals("global") || subdomain.equals("localhost")) {
            return ResponseEntity.ok(courseRepository.findAll());
        }
        return ResponseEntity.ok(courseRepository.findByTenantSubdomain(subdomain));
    }

    /** Delete any course */
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable("id") Long id) {
        courseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
