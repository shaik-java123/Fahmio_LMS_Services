package com.lms.repository;

import com.lms.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseId(Long courseId);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Enrollment e JOIN FETCH e.student JOIN FETCH e.course WHERE e.student.id = :studentId AND e.course.id = :courseId")
    Optional<Enrollment> findByStudentIdAndCourseId(@org.springframework.data.repository.query.Param("studentId") Long studentId, @org.springframework.data.repository.query.Param("courseId") Long courseId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    Long countByCourseId(Long courseId);

    Optional<Enrollment> findByStripeSubscriptionId(String stripeSubscriptionId);
    
    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM enrollments WHERE student_id = :studentId AND course_id = :courseId LIMIT 1", nativeQuery = true)
    Optional<Enrollment> findByStudentAndCourseIgnoreFilter(@org.springframework.data.repository.query.Param("studentId") Long studentId, @org.springframework.data.repository.query.Param("courseId") Long courseId);
    
    List<Enrollment> findAllByStatus(Enrollment.Status status);
}
