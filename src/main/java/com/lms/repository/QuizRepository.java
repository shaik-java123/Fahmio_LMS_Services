package com.lms.repository;

import com.lms.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByCourseIdAndPublishedTrue(Long courseId);
    List<Quiz> findByCourseId(Long courseId);
    List<Quiz> findByLessonId(Long lessonId);
}
