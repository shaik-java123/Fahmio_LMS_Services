package com.lms.controller;

import com.lms.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
public class InstructorController {

    private final AnalyticsService analyticsService;
    private final com.lms.repository.UserRepository userRepository;

    @GetMapping("/analytics/course/{courseId}/dropoffs")
    public ResponseEntity<List<Map<String, Object>>> getCourseDropOffs(@PathVariable Long courseId) {
        return ResponseEntity.ok(analyticsService.getCourseDropOffAnalytics(courseId));
    }
    
    @GetMapping("/analytics/my-stats")
    public ResponseEntity<com.lms.dto.InstructorAnalyticsDTO> getMyStats(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        
        com.lms.model.User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(analyticsService.getInstructorAnalytics(user.getId()));
    }
}
