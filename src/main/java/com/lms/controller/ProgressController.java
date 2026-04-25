package com.lms.controller;

import com.lms.dto.LessonProgressRequest;
import com.lms.model.LessonProgress;
import com.lms.security.JwtUtil;
import com.lms.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
@Tag(name = "Progress", description = "Student lesson and course progress tracking")
public class ProgressController {

    private final ProgressService progressService;
    private final JwtUtil jwtUtil;

    @PostMapping("/lesson")
    @Operation(summary = "Update progress for a lesson (called by video player heartbeat)")
    public ResponseEntity<Map<String, Object>> updateLessonProgress(
            @RequestBody LessonProgressRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(progressService.updateProgress(userId, request));
    }

    @GetMapping("/lesson/{lessonId}")
    @Operation(summary = "Get progress for a specific lesson")
    public ResponseEntity<LessonProgress> getLessonProgress(
            @PathVariable Long lessonId,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(progressService.getLessonProgress(userId, lessonId));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get overall course completion progress for the current user")
    public ResponseEntity<Map<String, Object>> getCourseProgress(
            @PathVariable Long courseId,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(progressService.getCourseProgress(userId, courseId));
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}
