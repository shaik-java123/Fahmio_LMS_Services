package com.lms.controller;

import com.lms.model.LiveClass;
import com.lms.security.JwtUtil;
import com.lms.service.LiveClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/live-classes")
@RequiredArgsConstructor
@Tag(name = "Live Classes", description = "Live session scheduling (Zoom/Meet)")
public class LiveClassController {

    private final LiveClassService liveClassService;
    private final JwtUtil jwtUtil;

    @PostMapping("/course/{courseId}")
    @Operation(summary = "Schedule a new live class for a course")
    public ResponseEntity<LiveClass> scheduleClass(
            @PathVariable Long courseId,
            @RequestBody LiveClass liveClass,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(liveClassService.scheduleClass(courseId, liveClass, userId));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get all live classes for a specific course")
    public ResponseEntity<List<LiveClass>> getCourseClasses(@PathVariable Long courseId) {
        return ResponseEntity.ok(liveClassService.getCourseClasses(courseId));
    }

    @GetMapping("/my-upcoming")
    @Operation(summary = "Get all upcoming live classes for the current student")
    public ResponseEntity<List<LiveClass>> getMyUpcomingClasses(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(liveClassService.getUpcomingClassesForUser(userId));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update the status of a live class (UPCOMING, LIVE, COMPLETED, CANCELLED)")
    public ResponseEntity<LiveClass> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserId(authHeader);
        LiveClass.ClassStatus status = LiveClass.ClassStatus.valueOf(payload.get("status").toUpperCase());
        return ResponseEntity.ok(liveClassService.updateStatus(id, status, userId));
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}
