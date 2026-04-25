package com.lms.service;

import com.lms.model.Course;
import com.lms.model.User;
import com.lms.repository.CourseRepository;
import com.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final TenantService tenantService;

    @Transactional
    public Course createCourse(Course course, Long instructorId) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        if (instructor.getRole() != User.Role.INSTRUCTOR && instructor.getRole() != User.Role.ADMIN && instructor.getRole() != User.Role.SUPER_ADMIN) {
            throw new RuntimeException("Only instructors or admins can create courses");
        }

        course.setInstructor(instructor);
        course.setTenant(tenantService.getCurrentTenant());
        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long courseId, Course courseDetails) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setTitle(courseDetails.getTitle());
        course.setDescription(courseDetails.getDescription());
        course.setCategory(courseDetails.getCategory());
        course.setLevel(courseDetails.getLevel());
        course.setThumbnail(courseDetails.getThumbnail());
        course.setDuration(courseDetails.getDuration());
        course.setPrice(courseDetails.getPrice());
        course.setPublished(courseDetails.isPublished());

        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public Course getCourseById(Long id) {
        String subdomain = com.lms.tenant.TenantContext.getCurrentTenant();
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
                
        // Verification logic
        if (!subdomain.equals("global") && !subdomain.equals("localhost")) {
             if (course.getTenant() != null && !course.getTenant().getSubdomain().equals(subdomain)) {
                 throw new RuntimeException("Access denied: This course belongs to another organization");
             }
        }
        return course;
    }

    @Transactional(readOnly = true)
    public List<Course> getAllPublishedCourses() {
        String subdomain = com.lms.tenant.TenantContext.getCurrentTenant();
        return courseRepository.findByPublishedTrueAndTenantSubdomain(subdomain);
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByInstructor(Long instructorId) {
        return courseRepository.findByInstructorId(instructorId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCoursesByInstructorWithEnrollmentCount(Long instructorId) {
        List<Object[]> rows = courseRepository.findByInstructorIdWithEnrollmentCount(instructorId);
        return rows.stream().map(row -> {
            Course course = (Course) row[0];
            Long count = (Long) row[1];
            Map<String, Object> map = new HashMap<>();
            map.put("id", course.getId());
            map.put("title", course.getTitle());
            map.put("description", course.getDescription());
            map.put("category", course.getCategory());
            map.put("level", course.getLevel());
            map.put("thumbnail", course.getThumbnail());
            map.put("duration", course.getDuration());
            map.put("price", course.getPrice());
            map.put("published", course.isPublished());
            map.put("createdAt", course.getCreatedAt());
            map.put("updatedAt", course.getUpdatedAt());
            map.put("enrollmentCount", count != null ? count : 0L);
            // Include instructor basic info
            if (course.getInstructor() != null) {
                Map<String, Object> instructor = new HashMap<>();
                instructor.put("id", course.getInstructor().getId());
                instructor.put("firstName", course.getInstructor().getFirstName());
                instructor.put("lastName", course.getInstructor().getLastName());
                instructor.put("email", course.getInstructor().getEmail());
                map.put("instructor", instructor);
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByCategory(String category) {
        String subdomain = com.lms.tenant.TenantContext.getCurrentTenant();
        return courseRepository.findByPublishedTrueAndCategoryAndTenantSubdomain(category, subdomain);
    }

    @Transactional
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    @Transactional
    public Course cloneCourse(Long sourceCourseId, Long instructorId) {
        Course source = courseRepository.findById(sourceCourseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        if (instructor.getRole() != User.Role.INSTRUCTOR && instructor.getRole() != User.Role.ADMIN && instructor.getRole() != User.Role.SUPER_ADMIN) {
            throw new RuntimeException("Only instructors or admins can clone courses");
        }

        Course clonedCourse = new Course();
        clonedCourse.setTitle(source.getTitle() + " (Copy)");
        clonedCourse.setDescription(source.getDescription());
        clonedCourse.setCategory(source.getCategory());
        clonedCourse.setLevel(source.getLevel());
        clonedCourse.setThumbnail(source.getThumbnail());
        clonedCourse.setDuration(source.getDuration());
        clonedCourse.setPrice(source.getPrice());
        clonedCourse.setPublished(false); // drafts
        clonedCourse.setInstructor(instructor);
        clonedCourse.setTenant(tenantService.getCurrentTenant());
        clonedCourse.getPrerequisites().addAll(source.getPrerequisites());

        for (com.lms.model.Module pm : source.getModules()) {
            com.lms.model.Module cm = new com.lms.model.Module();
            cm.setTitle(pm.getTitle());
            cm.setDescription(pm.getDescription());
            cm.setOrderIndex(pm.getOrderIndex());
            cm.setCourse(clonedCourse);
            
            for (com.lms.model.Lesson pl : pm.getLessons()) {
                com.lms.model.Lesson cl = new com.lms.model.Lesson();
                cl.setTitle(pl.getTitle());
                cl.setContent(pl.getContent());
                cl.setContentType(pl.getContentType());
                cl.setVideoUrl(pl.getVideoUrl());
                cl.setDocumentUrl(pl.getDocumentUrl());
                cl.setExternalUrl(pl.getExternalUrl());
                cl.setOrderIndex(pl.getOrderIndex());
                cl.setDurationMinutes(pl.getDurationMinutes());
                cl.setModule(cm);
                cl.getAttachments().addAll(pl.getAttachments());
                cl.getPrerequisites().addAll(pl.getPrerequisites());
                cm.getLessons().add(cl);
            }
            clonedCourse.getModules().add(cm);
        }
        
        return courseRepository.save(clonedCourse);
    }
}
