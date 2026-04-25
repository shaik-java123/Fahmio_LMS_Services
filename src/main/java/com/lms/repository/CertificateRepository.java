package com.lms.repository;

import com.lms.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByUserIdAndCourseId(Long userId, Long courseId);
    List<Certificate> findByUserId(Long userId);
    Optional<Certificate> findByVerificationCode(String verificationCode);
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
}
