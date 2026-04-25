package com.lms.service;

import com.lms.dto.LessonProgressRequest;
import com.lms.model.*;
import com.lms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateService certificateService;
    private final GamificationService gamificationService;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    /**
     * Record or update a student's progress on a specific lesson.
     * Returns the updated LessonProgress plus pointsAwarded (0 if already completed before).
     */
    @Transactional
    public Map<String, Object> updateProgress(Long userId, LessonProgressRequest req) {
        Lesson lesson = lessonRepository.findById(req.getLessonId())
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        LessonProgress progress = lessonProgressRepository
                .findByUserIdAndLessonId(userId, req.getLessonId())
                .orElseGet(() -> {
                    LessonProgress lp = new LessonProgress();
                    lp.setUser(new User());
                    lp.getUser().setId(userId);
                    lp.setLesson(lesson);
                    return lp;
                });

        progress.setLastPosition(req.getCurrentPosition() != null ? req.getCurrentPosition() : 0);

        if (req.getTotalDuration() != null && req.getTotalDuration() > 0) {
            int pct = (int) ((req.getCurrentPosition() * 100.0) / req.getTotalDuration());
            progress.setCompletionPercentage(Math.min(pct, 100));
            progress.setWatchedSeconds(req.getCurrentPosition());
        }

        int pointsAwarded = 0;

        // Mark as completed if watched >= 90%
        if (req.isCompleted() || progress.getCompletionPercentage() >= 90) {
            if (!progress.isCompleted()) {
                progress.setCompleted(true);
                progress.setCompletedAt(LocalDateTime.now());
                log.info("User {} completed lesson {}", userId, req.getLessonId());

                // Gamification: Award base points for lesson completion
                gamificationService.addPoints(userId, GamificationService.Activity.LESSON_COMPLETED);
                pointsAwarded += GamificationService.Activity.LESSON_COMPLETED.points;

                // Award extra lesson-specific points configured by the instructor
                int lessonBonus = lesson.getLearningPoints() != null ? lesson.getLearningPoints() : 0;
                if (lessonBonus > 0) {
                    gamificationService.addBonusPoints(userId, lessonBonus);
                    pointsAwarded += lessonBonus;
                    log.info("Awarded {} bonus lesson points to user {} for lesson {}", lessonBonus, userId, lesson.getId());
                }

                // Check if the whole course is now done
                Long courseId = lesson.getModule().getCourse().getId();
                checkAndIssueCertificate(userId, courseId);
            }
        }

        LessonProgress saved = lessonProgressRepository.save(progress);
        Map<String, Object> result = new HashMap<>();
        result.put("progress", saved);
        result.put("pointsAwarded", pointsAwarded);
        result.put("totalPoints", gamificationService.getTotalPoints(userId));
        return result;
    }

    /**
     * Returns a summary of course completion for a student.
     */
    @Transactional
    public Map<String, Object> getCourseProgress(Long userId, Long courseId) {
        // Fix potential stuck enrollments
        checkAndIssueCertificate(userId, courseId);
        
        long total = lessonProgressRepository.countTotalLessonsInCourse(courseId);
        long completed = lessonProgressRepository.countCompletedLessonsInCourse(userId, courseId);

        int percentage = total > 0 ? (int) ((completed * 100.0) / total) : 0;
        boolean hasCertificate = certificateRepository.existsByUserIdAndCourseId(userId, courseId);

        Long lastAccessedLessonId = lessonProgressRepository
                .findFirstByUserIdAndLesson_Module_CourseIdOrderByIdDesc(userId, courseId)
                .map(lp -> lp.getLesson().getId())
                .orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("totalLessons", total);
        result.put("completedLessons", completed);
        result.put("percentage", percentage);
        result.put("hasCertificate", hasCertificate);
        result.put("lastAccessedLessonId", lastAccessedLessonId);
        return result;
    }

    @Transactional(readOnly = true)
    public LessonProgress getLessonProgress(Long userId, Long lessonId) {
        return lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    LessonProgress lp = new LessonProgress();
                    lp.setCompletionPercentage(0);
                    lp.setLastPosition(0);
                    return lp;
                });
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    private void checkAndIssueCertificate(Long userId, Long courseId) {
        long total = lessonProgressRepository.countTotalLessonsInCourse(courseId);
        long completed = lessonProgressRepository.countCompletedLessonsInCourse(userId, courseId);

        // Update overall enrollment percentage
        enrollmentRepository.findByStudentIdAndCourseId(userId, courseId).ifPresent(e -> {
            int pct = total > 0 ? (int) ((completed * 100.0) / total) : 0;
            e.setProgress(pct);
            
            if (total > 0 && completed >= total) {
                if (e.getStatus() != Enrollment.Status.COMPLETED) {
                    e.setStatus(Enrollment.Status.COMPLETED);
                    e.setCompletedAt(LocalDateTime.now());
                    log.info("Course {} marked as COMPLETED for user {}", courseId, userId);
                    
                    // Gamification: Award Points for course completion
                    gamificationService.addPoints(userId, GamificationService.Activity.COURSE_COMPLETED);
                }

                if (!certificateRepository.existsByUserIdAndCourseId(userId, courseId)) {
                    // Check if all assignments are completed
                    java.util.List<Assignment> assignments = assignmentRepository.findByCourseId(courseId);
                    boolean allAssignmentsDone = true;
                    for (Assignment a : assignments) {
                        if (submissionRepository.findByAssignmentIdAndStudentId(a.getId(), userId).isEmpty()) {
                            log.info("User {} hasn't submitted assignment {} for course {}", userId, a.getId(), courseId);
                            allAssignmentsDone = false;
                            break;
                        }
                    }

                    if (allAssignmentsDone) {
                        log.info("Issuing certificate for user {} in course {}", userId, courseId);
                        certificateService.issueCertificate(userId, courseId);
                    } else {
                        log.info("User {} has completed all lessons but not all assignments in course {}. Skipping certificate issuance.", userId, courseId);
                    }
                }
            }
            enrollmentRepository.save(e);
        });
    }
}
