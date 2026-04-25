package com.lms.repository;

import com.lms.model.EmailVerification;
import com.lms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByTokenAndUsedFalse(String token);

    @Modifying
    @Query("UPDATE EmailVerification e SET e.used = true WHERE e.user = :user")
    void invalidateAllByUser(User user);
}
