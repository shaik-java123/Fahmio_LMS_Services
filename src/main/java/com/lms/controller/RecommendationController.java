package com.lms.controller;

import com.lms.model.Course;
import com.lms.security.JwtUtil;
import com.lms.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "AI Recommendations", description = "Personalized and trending course suggestions")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final JwtUtil jwtUtil;

    @GetMapping("/personalized")
    @Operation(summary = "Get course recommendations for the current user based on interests")
    public ResponseEntity<List<Course>> getPersonalized(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(recommendationService.getPersonalizedRecommendations(userId));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get globally trending courses")
    public ResponseEntity<List<Course>> getTrending() {
        return ResponseEntity.ok(recommendationService.getTrendingCourses());
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}
