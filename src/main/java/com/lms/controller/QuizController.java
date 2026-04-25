package com.lms.controller;

import com.lms.dto.QuizRequest;
import com.lms.dto.QuizSubmitRequest;
import com.lms.model.Quiz;
import com.lms.model.QuizAttempt;
import com.lms.security.JwtUtil;
import com.lms.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "Quiz management and student attempts")
public class QuizController {

    private final QuizService quizService;
    private final JwtUtil jwtUtil;

    // ── Instructor / Admin endpoints ─────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    @Operation(summary = "Create a new quiz for a course")
    public ResponseEntity<Quiz> createQuiz(@RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.createQuiz(request));
    }

    @PutMapping("/{quizId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    @Operation(summary = "Update an existing quiz and its questions")
    public ResponseEntity<Quiz> updateQuiz(@PathVariable Long quizId,
                                           @RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.updateQuiz(quizId, request));
    }

    @PutMapping("/{quizId}/publish")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    @Operation(summary = "Publish or unpublish a quiz")
    public ResponseEntity<Void> publishQuiz(@PathVariable Long quizId,
                                            @RequestParam boolean published) {
        quizService.publishQuiz(quizId, published);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{quizId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    @Operation(summary = "Delete a quiz")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long quizId) {
        quizService.deleteQuiz(quizId);
        return ResponseEntity.noContent().build();
    }

    // ── Student / Public endpoints ───────────────────────────────────────────

    @GetMapping("/{quizId}")
    @Operation(summary = "Get quiz details (questions without answers)")
    public ResponseEntity<Quiz> getQuiz(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.getQuiz(quizId));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "List all published quizzes for a course")
    public ResponseEntity<List<Quiz>> getQuizzesForCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(quizService.getQuizzesForCourse(courseId));
    }

    @GetMapping("/lesson/{lessonId}")
    @Operation(summary = "Get the quiz associated with a specific lesson")
    public ResponseEntity<Quiz> getQuizByLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(quizService.getQuizByLesson(lessonId));
    }

    @PostMapping("/{quizId}/attempts/start")
    @Operation(summary = "Start a new quiz attempt")
    public ResponseEntity<QuizAttempt> startAttempt(@PathVariable Long quizId,
                                                     @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(quizService.startAttempt(quizId, userId));
    }

    @PostMapping("/attempts/submit")
    @Operation(summary = "Submit answers for a quiz attempt")
    public ResponseEntity<QuizAttempt> submitAttempt(@RequestBody QuizSubmitRequest request) {
        return ResponseEntity.ok(quizService.submitAttempt(request));
    }

    @GetMapping("/{quizId}/attempts")
    @Operation(summary = "Get all attempts for the current user on a quiz")
    public ResponseEntity<List<QuizAttempt>> getAttempts(@PathVariable Long quizId,
                                                         @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(quizService.getUserAttempts(quizId, userId));
    }

    @GetMapping("/{quizId}/attempts/best")
    @Operation(summary = "Get the student's best attempt on a quiz")
    public ResponseEntity<QuizAttempt> getBestAttempt(@PathVariable Long quizId,
                                                       @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(quizService.getBestAttempt(quizId, userId));
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}
