package com.lms.repository;

import com.lms.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUserIdAndQuizId(Long userId, Long quizId);
    List<QuizAttempt> findByUserId(Long userId);
    int countByUserIdAndQuizId(Long userId, Long quizId);
    Optional<QuizAttempt> findTopByUserIdAndQuizIdOrderByScoreDesc(Long userId, Long quizId);
    boolean existsByUserIdAndQuizIdAndPassedTrue(Long userId, Long quizId);
}
