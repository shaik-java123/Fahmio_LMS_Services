package com.lms.repository;

import com.lms.model.SupportRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    Optional<SupportRequest> findByTicketId(String ticketId);

    Page<SupportRequest> findByEmail(String email, Pageable pageable);

    Page<SupportRequest> findByStatus(SupportRequest.Status status, Pageable pageable);

    Page<SupportRequest> findByCategory(SupportRequest.Category category, Pageable pageable);

    Page<SupportRequest> findByPriority(SupportRequest.Priority priority, Pageable pageable);

    Page<SupportRequest> findByAssignedTo(String assignedTo, Pageable pageable);

    List<SupportRequest> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<SupportRequest> findByStatusAndCreatedAtBefore(SupportRequest.Status status, LocalDateTime date);

    long countByStatus(SupportRequest.Status status);

    long countByPriority(SupportRequest.Priority priority);

    long countByCategory(SupportRequest.Category category);

    Page<SupportRequest> findByStatusOrCategory(
        SupportRequest.Status status,
        SupportRequest.Category category,
        Pageable pageable
    );
}

