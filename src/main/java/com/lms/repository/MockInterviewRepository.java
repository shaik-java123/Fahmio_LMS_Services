package com.lms.repository;

import com.lms.model.MockInterview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MockInterviewRepository extends JpaRepository<MockInterview, Long> {
    List<MockInterview> findByUserIdOrderByStartedAtDesc(Long userId);
    List<MockInterview> findByUserIdAndStatusOrderByStartedAtDesc(Long userId, MockInterview.Status status);
    long countByUserIdAndStatus(Long userId, MockInterview.Status status);
}

