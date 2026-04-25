package com.lms.service;

import com.lms.model.Course;
import com.lms.model.LiveClass;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.LiveClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LiveClassService {

    private final LiveClassRepository liveClassRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public LiveClass scheduleClass(Long courseId, LiveClass liveClass, Long instructorId) {
        Course course = courseRepository.findById(courseId.longValue())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("Only the instructor can schedule live classes");
        }

        liveClass.setCourse(course);
        return liveClassRepository.save(liveClass);
    }

    @Transactional(readOnly = true)
    public List<LiveClass> getCourseClasses(Long courseId) {
        return liveClassRepository.findByCourseIdOrderByStartTime(courseId);
    }

    @Transactional(readOnly = true)
    public List<LiveClass> getUpcomingClassesForUser(Long userId) {
        List<Long> enrolledCourseIds = enrollmentRepository.findByStudentId(userId).stream()
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toList());

        // Simple way: Fetch all and filter (or use @Query)
        return liveClassRepository.findAll().stream()
                .filter(lc -> enrolledCourseIds.contains(lc.getCourse().getId()))
                .filter(lc -> lc.getStatus() == LiveClass.ClassStatus.UPCOMING || lc.getStatus() == LiveClass.ClassStatus.LIVE)
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());
    }

    @Transactional
    public LiveClass updateStatus(Long classId, LiveClass.ClassStatus status, Long instructorId) {
        LiveClass lc = liveClassRepository.findById(classId.longValue())
                .orElseThrow(() -> new RuntimeException("Live class not found"));

        if (!lc.getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("Not authorized");
        }

        lc.setStatus(status);
        return liveClassRepository.save(lc);
    }
}
