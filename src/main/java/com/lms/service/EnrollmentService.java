package com.lms.service;

import com.lms.model.Course;
import com.lms.model.Enrollment;
import com.lms.model.User;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final NotificationService notificationService;

    @Transactional
    public Enrollment enrollStudent(Long studentId, Long courseId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Use the ignore-filter query to find existing enrollments even if they're in a different (or missing) tenant context
        java.util.Optional<Enrollment> existing = enrollmentRepository.findByStudentAndCourseIgnoreFilter(studentId, courseId);
        
        Enrollment enrollment;
        if (existing.isPresent()) {
            enrollment = existing.get();
            // If it exists but is tagged elsewhere, 'move' it to this tenant and reactivate it
            enrollment.setTenant(course.getTenant());
            enrollment.setStatus(Enrollment.Status.ACTIVE);
            log.info("Recovered ghost enrollment for user {} in course {}.", student.getEmail(), course.getTitle());
        } else {
            enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(course);
            enrollment.setTenant(course.getTenant());
            enrollment.setStatus(Enrollment.Status.ACTIVE);
        }

        try {
            Enrollment saved = enrollmentRepository.save(enrollment);
            
            // Notify student
            notificationService.createNotification(
                studentId, 
                "Course Assigned: " + course.getTitle(), 
                "You have been assigned to " + course.getTitle() + ". Start learning now!", 
                com.lms.model.Notification.Type.ENROLLMENT
            );
            
            return saved;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Race condition during manual enroll: User {} Course {}. Returning existing record if possible.", studentId, courseId);
            return existing.orElse(enrollment);
        }
        
    }

    @Transactional
    public void updateProgress(Long enrollmentId, Integer progress) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        enrollment.setProgress(progress);

        if (progress >= 100) {
            enrollment.setStatus(Enrollment.Status.COMPLETED);
            enrollment.setCompletedAt(LocalDateTime.now());
        }

        enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getStudentEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getCourseEnrollments(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @Transactional
    public void unenroll(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        enrollment.setStatus(Enrollment.Status.DROPPED);
        enrollmentRepository.save(enrollment);
    }
}
