package com.lms.service;

import com.lms.model.*;
import com.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscussionService {

    private final DiscussionTopicRepository topicRepository;
    private final DiscussionCommentRepository commentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    @Transactional
    public DiscussionTopic createTopic(Long userId, Long courseId, Long lessonId, String title, String content) {
        if (userId == null || courseId == null) {
            throw new IllegalArgumentException("User ID and Course ID must not be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        DiscussionTopic topic = new DiscussionTopic();
        topic.setAuthor(user);
        topic.setCourse(course);
        topic.setTitle(title);
        topic.setContent(content);

        if (lessonId != null) {
            Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
            topic.setLesson(lesson);
        }

        return topicRepository.save(topic);
    }

    @Transactional
    public DiscussionComment createComment(Long userId, Long topicId, String content) {
        if (userId == null || topicId == null) {
            throw new IllegalArgumentException("User ID and Topic ID must not be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        DiscussionTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        DiscussionComment comment = new DiscussionComment();
        comment.setAuthor(user);
        comment.setTopic(topic);
        comment.setContent(content);
        
        // Check if user is the course instructor
        if (topic.getCourse().getInstructor().getId().equals(userId)) {
            comment.setInstructorReply(true);
        }

        return commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<DiscussionTopic> getCourseTopics(Long courseId) {
        return topicRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
    }

    @Transactional(readOnly = true)
    public List<DiscussionTopic> getLessonTopics(Long lessonId) {
        return topicRepository.findByLessonIdOrderByCreatedAtDesc(lessonId);
    }

    @Transactional(readOnly = true)
    public DiscussionTopic getTopicDetails(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));
    }

    @Transactional
    public void incrementLikes(Long topicId) {
        DiscussionTopic topic = getTopicDetails(topicId);
        topic.setLikesCount(topic.getLikesCount() + 1);
        topicRepository.save(topic);
    }
}
