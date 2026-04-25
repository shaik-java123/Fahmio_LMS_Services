package com.lms.service;

import com.lms.model.Assignment;
import com.lms.model.Course;
import com.lms.model.Submission;
import com.lms.model.User;
import com.lms.repository.AssignmentRepository;
import com.lms.repository.CourseRepository;
import com.lms.repository.SubmissionRepository;
import com.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Transactional
    public Assignment createAssignment(Long courseId, Assignment assignment, Long userId) {
        if (courseId == null) throw new RuntimeException("Course ID is required");
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!course.getInstructor().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to create assignments for this course");
        }

        assignment.setCourse(course);
        return assignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<Assignment> getAssignmentsByCourse(Long courseId) {
        return assignmentRepository.findByCourseId(courseId);
    }

    @Transactional
    public Submission submitAssignment(long assignmentId, long studentId, String content, String fileUrl, Integer fileSizeKb) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if already submitted
        if (submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId).isPresent()) {
            throw new RuntimeException("You have already submitted this assignment");
        }

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setContent(content);
        submission.setFileUrl(fileUrl);
        submission.setFileSizeKb(fileSizeKb);

        // Simulation: Plagiarism Engine
        if (Boolean.TRUE.equals(assignment.getPlagiarismCheckEnabled())) {
            // Generate a random score for demonstration (0.0 to 1.0)
            double score = 0.05 + (Math.random() * 0.15); // Default low similarity
            if (content != null && content.length() < 50 && assignment.getType() == Assignment.AssignmentType.CODE) {
                score += 0.3; // Short code often has higher similarity
            }
            submission.setPlagiarismScore(Math.min(score, 1.0));
        }

        if (LocalDateTime.now().isAfter(assignment.getDueDate())) {
            submission.setStatus(Submission.Status.LATE);
        }

        Submission saved = submissionRepository.save(submission);
        
        // Notify Instructor
        notificationService.createNotification(
            assignment.getCourse().getInstructor().getId(),
            "New Submission: " + assignment.getTitle(),
            student.getFirstName() + " submitted an assignment for " + assignment.getCourse().getTitle(),
            com.lms.model.Notification.Type.ASSIGNMENT
        );

        emailService.sendAssignmentSubmissionNotification(
            assignment.getCourse().getInstructor().getEmail(),
            assignment.getCourse().getInstructor().getFirstName(),
            student.getFirstName(),
            assignment.getTitle(),
            assignment.getCourse().getTitle()
        );

        return saved;
    }

    @Transactional
    public Submission gradeSubmission(long submissionId, Integer grade, String feedback, long instructorId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        if (!submission.getAssignment().getCourse().getInstructor().getId().equals(instructorId) &&
            instructor.getRole() != User.Role.ADMIN && 
            instructor.getRole() != User.Role.SUPER_ADMIN) {
            throw new RuntimeException("Not authorized to grade this submission");
        }

        submission.setGrade(grade);
        submission.setFeedback(feedback);
        submission.setGradedAt(LocalDateTime.now());
        submission.setStatus(Submission.Status.GRADED);

        Submission saved = submissionRepository.save(submission);
        
        // Notify Student
        notificationService.createNotification(
            submission.getStudent().getId(),
            "Assignment Graded: " + submission.getAssignment().getTitle(),
            "Your assignment has been graded. Grade: " + grade + "/100",
            com.lms.model.Notification.Type.ASSIGNMENT
        );

        emailService.sendGradeNotification(
            submission.getStudent().getEmail(),
            submission.getStudent().getFirstName(),
            submission.getAssignment().getTitle(),
            grade,
            feedback
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Submission> getSubmissionsByAssignment(long assignmentId, long userId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (assignment.getCourse().getInstructor().getId().equals(userId) || 
            user.getRole() == User.Role.ADMIN || 
            user.getRole() == User.Role.SUPER_ADMIN) {
            // Instructor and Admins see all
            return submissionRepository.findByAssignmentId(assignmentId);
        } else {
            // Student sees only their own
            return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, userId)
                    .map(List::of)
                    .orElse(List.of());
        }
    }
}
