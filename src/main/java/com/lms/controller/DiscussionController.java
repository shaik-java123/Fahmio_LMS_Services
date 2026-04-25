package com.lms.controller;

import com.lms.model.DiscussionComment;
import com.lms.model.DiscussionTopic;
import com.lms.security.JwtUtil;
import com.lms.service.DiscussionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discussions")
@RequiredArgsConstructor
@Tag(name = "Discussions", description = "Course and lesson discussion boards")
public class DiscussionController {

    private final DiscussionService discussionService;
    private final JwtUtil jwtUtil;

    @PostMapping("/topic")
    @Operation(summary = "Create a new discussion topic/thread")
    public ResponseEntity<DiscussionTopic> createTopic(
            @RequestBody Map<String, Object> req,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        Long courseId = Long.valueOf(req.get("courseId").toString());
        Long lessonId = req.get("lessonId") != null ? Long.valueOf(req.get("lessonId").toString()) : null;
        String title = req.get("title").toString();
        String content = req.get("content").toString();

        return ResponseEntity.ok(discussionService.createTopic(userId, courseId, lessonId, title, content));
    }

    @PostMapping("/comment")
    @Operation(summary = "Post a comment in a discussion topic")
    public ResponseEntity<DiscussionComment> createComment(
            @RequestBody Map<String, Object> req,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        Long topicId = Long.valueOf(req.get("topicId").toString());
        String content = req.get("content").toString();

        return ResponseEntity.ok(discussionService.createComment(userId, topicId, content));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get all discussion topics for a course")
    public ResponseEntity<List<DiscussionTopic>> getCourseTopics(@PathVariable Long courseId) {
        return ResponseEntity.ok(discussionService.getCourseTopics(courseId));
    }

    @GetMapping("/lesson/{lessonId}")
    @Operation(summary = "Get all discussion topics for a specific lesson")
    public ResponseEntity<List<DiscussionTopic>> getLessonTopics(@PathVariable Long lessonId) {
        return ResponseEntity.ok(discussionService.getLessonTopics(lessonId));
    }

    @GetMapping("/topic/{topicId}")
    @Operation(summary = "Get a single topic with all its comments")
    public ResponseEntity<DiscussionTopic> getTopicDetails(@PathVariable Long topicId) {
        return ResponseEntity.ok(discussionService.getTopicDetails(topicId));
    }

    @PostMapping("/topic/{topicId}/like")
    @Operation(summary = "Like a discussion topic")
    public ResponseEntity<Void> likeTopic(@PathVariable Long topicId) {
        discussionService.incrementLikes(topicId);
        return ResponseEntity.ok().build();
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}
