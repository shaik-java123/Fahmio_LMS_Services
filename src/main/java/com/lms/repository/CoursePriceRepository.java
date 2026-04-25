package com.lms.repository;

import com.lms.model.CoursePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoursePriceRepository extends JpaRepository<CoursePrice, Long> {

    Optional<CoursePrice> findByCourseId(Long courseId);
}
