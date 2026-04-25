package com.lms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "assignment_allocations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @Column(nullable = false)
    private Boolean notificationSent = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime allocatedAt;

    @Column
    private LocalDateTime notificationSentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status = AllocationStatus.ASSIGNED;

    public enum AllocationStatus {
        ASSIGNED,
        IN_PROGRESS,
        SUBMITTED,
        GRADED,
        OVERDUE
    }
}

