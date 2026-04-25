package com.lms.controller;

import com.lms.dto.InstructorAnalyticsDTO;
import com.lms.security.JwtUtil;
import com.lms.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Instructor dashboard statistics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final JwtUtil jwtUtil;

    @GetMapping("/instructor")
    @Operation(summary = "Get performance and revenue stats for the instructor dashboard")
    public ResponseEntity<InstructorAnalyticsDTO> getInstructorStats(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(analyticsService.getInstructorAnalytics(userId));
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}
