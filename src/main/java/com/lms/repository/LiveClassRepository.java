package com.lms.repository;

import com.lms.model.LiveClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveClassRepository extends JpaRepository<LiveClass, Long> {
    List<LiveClass> findByCourseIdOrderByStartTime(Long courseId);
    List<LiveClass> findByCourseIdAndStatusOrderByStartTime(Long courseId, LiveClass.ClassStatus status);
}
