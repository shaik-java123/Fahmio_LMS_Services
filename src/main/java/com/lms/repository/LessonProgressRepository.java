package com.lms.repository;

import com.lms.model.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);
    List<LessonProgress> findByUserId(Long userId);
    Optional<LessonProgress> findFirstByUserIdAndLesson_Module_CourseIdOrderByIdDesc(Long userId, Long courseId);

    @Query("SELECT COUNT(lp) FROM LessonProgress lp " +
           "JOIN lp.lesson l JOIN l.module m " +
           "WHERE lp.user.id = :userId AND m.course.id = :courseId AND lp.completed = true")
    long countCompletedLessonsInCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("SELECT COUNT(l) FROM Lesson l JOIN l.module m WHERE m.course.id = :courseId")
    long countTotalLessonsInCourse(@Param("courseId") Long courseId);

    long countByLessonId(Long lessonId);
    long countByLessonIdAndCompletedTrue(Long lessonId);
}
