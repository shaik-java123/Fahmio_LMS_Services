package com.lms.repository;

import com.lms.model.DiscussionTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionTopicRepository extends JpaRepository<DiscussionTopic, Long> {
    List<DiscussionTopic> findByCourseIdOrderByCreatedAtDesc(Long courseId);
    List<DiscussionTopic> findByLessonIdOrderByCreatedAtDesc(Long lessonId);
}
