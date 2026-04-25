package com.lms.service;

import com.lms.model.Course;
import com.lms.model.Enrollment;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Recommends courses based on user's interests (categories of courses they are already in).
     */
    public List<Course> getPersonalizedRecommendations(Long userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(userId);
        
        if (enrollments.isEmpty()) {
            return getTrendingCourses();
        }

        // Get categories I'm interested in
        Set<String> interestedCategories = enrollments.stream()
                .map(e -> e.getCourse().getCategory())
                .collect(Collectors.toSet());

        Set<Long> enrolledCourseIds = enrollments.stream()
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toSet());

        // Find courses in those categories I'm NOT yet enrolled in
        List<Course> recommendations = courseRepository.findAll().stream()
                .filter(c -> interestedCategories.contains(c.getCategory()))
                .filter(c -> !enrolledCourseIds.contains(c.getId()))
                .filter(Course::isPublished)
                .limit(6)
                .collect(Collectors.toList());

        if (recommendations.size() < 3) {
            // Fill with trending courses if personalized ones are sparse
            List<Course> trending = getTrendingCourses();
            for (Course c : trending) {
                if (!enrolledCourseIds.contains(c.getId()) && !recommendations.contains(c)) {
                    recommendations.add(c);
                }
                if (recommendations.size() >= 6) break;
            }
        }

        return recommendations;
    }

    public List<Course> getTrendingCourses() {
        // Simple logic: Trending = top rated (mock logic for now)
        return courseRepository.findAll().stream()
                .filter(Course::isPublished)
                .limit(10)
                .collect(Collectors.toList());
    }
}
