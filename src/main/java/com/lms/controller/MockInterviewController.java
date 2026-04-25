package com.lms.controller;

import com.lms.dto.StartInterviewRequest;
import com.lms.dto.SubmitAnswerRequest;
import com.lms.dto.SubmitVoiceAnswerRequest;
import com.lms.model.InterviewQuestion;
import com.lms.model.MockInterview;
import com.lms.security.JwtUtil;
import com.lms.service.MockInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class MockInterviewController {

    private final MockInterviewService interviewService;
    private final JwtUtil jwtUtil;

    /** Start a new mock interview (TEXT or VOICE) */
    @PostMapping("/start")
    public ResponseEntity<MockInterview> startInterview(
            @RequestBody StartInterviewRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        MockInterview interview = interviewService.startInterview(
                userId, request.getTopic(), request.getDifficulty(),
                request.getTotalQuestions(), request.getInterviewType());
        return ResponseEntity.ok(interview);
    }

    /** Submit an answer to the current question, receive evaluation + next question */
    @PostMapping("/{interviewId}/answer")
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @PathVariable Long interviewId,
            @RequestBody SubmitAnswerRequest request,
            @RequestHeader("Authorization") String authHeader) {
        extractUserId(authHeader); // security check
        Map<String, Object> result = interviewService.submitAnswer(
                interviewId, request.getQuestionId(), request.getAnswer());
        return ResponseEntity.ok(result);
    }

    /** Submit a voice-based answer, transcribe it, and get AI evaluation */
    @PostMapping("/{interviewId}/voice-answer")
    public ResponseEntity<Map<String, Object>> submitVoiceAnswer(
            @PathVariable Long interviewId,
            @RequestBody SubmitVoiceAnswerRequest request,
            @RequestHeader("Authorization") String authHeader) {
        extractUserId(authHeader); // security check
        Map<String, Object> result = interviewService.submitVoiceAnswer(
                interviewId, request.getQuestionId(), request.getVoiceAnswerUrl(),
                request.getVoiceDurationSeconds(), request.getVoiceTranscription());
        return ResponseEntity.ok(result);
    }

    /** Mark interview as complete and get overall AI summary */
    @PostMapping("/{interviewId}/complete")
    public ResponseEntity<MockInterview> completeInterview(
            @PathVariable Long interviewId,
            @RequestHeader("Authorization") String authHeader) {
        extractUserId(authHeader);
        return ResponseEntity.ok(interviewService.completeInterview(interviewId));
    }

    /** Abandon (quit) an interview */
    @PostMapping("/{interviewId}/abandon")
    public ResponseEntity<Void> abandonInterview(
            @PathVariable Long interviewId,
            @RequestHeader("Authorization") String authHeader) {
        extractUserId(authHeader);
        interviewService.abandonInterview(interviewId);
        return ResponseEntity.ok().build();
    }

    /** Get all past interviews for current user */
    @GetMapping("/my")
    public ResponseEntity<List<MockInterview>> getMyInterviews(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(interviewService.getUserInterviews(userId));
    }

    /** Get full interview details + questions */
    @GetMapping("/{interviewId}")
    public ResponseEntity<MockInterview> getInterview(
            @PathVariable Long interviewId,
            @RequestHeader("Authorization") String authHeader) {
        extractUserId(authHeader);
        return ResponseEntity.ok(interviewService.getInterviewById(interviewId));
    }

    /** Get all questions for an interview (with answers/scores) */
    @GetMapping("/{interviewId}/questions")
    public ResponseEntity<List<InterviewQuestion>> getQuestions(
            @PathVariable Long interviewId,
            @RequestHeader("Authorization") String authHeader) {
        extractUserId(authHeader);
        return ResponseEntity.ok(interviewService.getInterviewQuestions(interviewId));
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}

