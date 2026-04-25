package com.lms.repository;

import com.lms.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("SELECT c FROM Course c WHERE c.instructor.id = :instructorId")
    List<Course> findByInstructorId(@Param("instructorId") Long instructorId);

    /** Returns [Course, enrollmentCount] pairs for an instructor */
    @Query("SELECT c, COUNT(e.id) FROM Course c LEFT JOIN Enrollment e ON e.course.id = c.id WHERE c.instructor.id = :instructorId GROUP BY c")
    List<Object[]> findByInstructorIdWithEnrollmentCount(@Param("instructorId") Long instructorId);

    List<Course> findByPublishedTrue();
    long countByPublishedTrue();
    
    List<Course> findByTenantSubdomain(String subdomain);
    
    @Query("SELECT c FROM Course c WHERE c.published = true AND " +
           "(:subdomain = 'global' OR :subdomain = 'localhost' OR c.tenant IS NULL OR c.tenant.subdomain = :subdomain)")
    List<Course> findByPublishedTrueAndTenantSubdomain(@Param("subdomain") String subdomain);
    
    @Query("SELECT c FROM Course c WHERE c.published = true AND c.category = :category AND " +
           "(:subdomain = 'global' OR :subdomain = 'localhost' OR c.tenant IS NULL OR c.tenant.subdomain = :subdomain)")
    List<Course> findByPublishedTrueAndCategoryAndTenantSubdomain(@Param("category") String category, @Param("subdomain") String subdomain);
}


