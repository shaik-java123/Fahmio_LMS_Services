package com.lms.controller;

import com.lms.model.Review;
import com.lms.security.JwtUtil;
import com.lms.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Student course reviews and ratings")
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtUtil jwtUtil;

    @PostMapping("/course/{courseId}")
    @Operation(summary = "Add a review for a course")
    public ResponseEntity<Review> addReview(
            @PathVariable Long courseId,
            @RequestBody Review review,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(reviewService.addReview(courseId, userId, review.getRating(), review.getComment()));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get all reviews for a specific course")
    public ResponseEntity<List<Review>> getReviews(@PathVariable Long courseId) {
        return ResponseEntity.ok(reviewService.getCourseReviews(courseId));
    }

    @GetMapping("/course/{courseId}/summary")
    @Operation(summary = "Get average rating and total reviews for a course")
    public ResponseEntity<Map<String, Object>> getSummary(@PathVariable Long courseId) {
        return ResponseEntity.ok(reviewService.getCourseRatingSummary(courseId));
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}
