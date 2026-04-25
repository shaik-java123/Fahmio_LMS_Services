package com.lms.service;

import com.lms.model.Course;
import com.lms.model.Review;
import com.lms.model.User;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ReviewRepository;
import com.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EmailService emailService;

    @Transactional
    public Review addReview(Long courseId, Long userId, Integer rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        // Check if already reviewed
        if (reviewRepository.existsByStudentIdAndCourseId(userId, courseId)) {
            throw new RuntimeException("You have already reviewed this course");
        }

        // Check if enrolled
        if (!enrollmentRepository.existsByStudentIdAndCourseId(userId, courseId)) {
            throw new RuntimeException("You must be enrolled to review this course");
        }

        Course course = courseRepository.findById(courseId.longValue())
                .orElseThrow(() -> new RuntimeException("Course not found"));
        User user = userRepository.findById(userId.longValue())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Review review = new Review();
        review.setCourse(course);
        review.setStudent(user);
        review.setRating(rating);
        review.setComment(comment);

        Review saved = reviewRepository.save(review);
        
        // Notify instructor
        emailService.sendReviewNotification(
            course.getInstructor().getEmail(),
            course.getInstructor().getFirstName(),
            user.getFirstName(),
            course.getTitle(),
            rating
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Review> getCourseReviews(Long courseId) {
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCourseRatingSummary(Long courseId) {
        Double avg = reviewRepository.getAverageRatingForCourse(courseId);
        Long count = (long) getCourseReviews(courseId).size();
        
        return Map.of(
            "averageRating", avg != null ? avg : 0.0,
            "totalReviews", count
        );
    }
}
