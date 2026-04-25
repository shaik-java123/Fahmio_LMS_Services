package com.lms.controller;

import com.lms.model.Enrollment;
import com.lms.model.User;
import com.lms.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final com.lms.repository.UserRepository userRepository;

    @PostMapping("/course/{courseId}")
    public ResponseEntity<Enrollment> enroll(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(enrollmentService.enrollStudent(user.getId(), courseId));
    }

    @GetMapping("/my-enrollments")
    public ResponseEntity<List<Enrollment>> getMyEnrollments(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(enrollmentService.getStudentEnrollments(user.getId()));
    }

    @PostMapping("/course/{courseId}/assign")
    public ResponseEntity<Enrollment> assign(
            @PathVariable Long courseId,
            @RequestBody String email) {
        
        // Remove quotes if present from JSON string
        String cleanEmail = email.replace("\"", "");
        User student = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new RuntimeException("Student with email " + cleanEmail + " not found"));
                
        return ResponseEntity.ok(enrollmentService.enrollStudent(student.getId(), courseId));
    }

    @GetMapping("/course/{courseId}/students")
    public ResponseEntity<List<Enrollment>> getCourseStudents(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.getCourseEnrollments(courseId));
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<Void> updateProgress(@PathVariable Long id, @RequestBody Integer progress) {
        enrollmentService.updateProgress(id, progress);
        return ResponseEntity.ok().build();
    }
}
