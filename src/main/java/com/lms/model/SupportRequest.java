package com.lms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private Category category = Category.GENERAL;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(name = "support_ticket_id", unique = true)
    private String ticketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private Priority priority = Priority.MEDIUM;

    private String assignedTo;

    @Column(length = 2000)
    private String adminResponse;

    private LocalDateTime respondedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean emailSent = false;

    // Enums
    public enum Category {
        GENERAL("General Inquiry"),
        TECHNICAL("Technical Issue"),
        BILLING("Billing & Payment"),
        COURSE("Course Content"),
        ACCOUNT("Account & Profile"),
        FEATURE("Feature Request");

        public final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum Status {
        OPEN("Open"),
        IN_PROGRESS("In Progress"),
        WAITING_CUSTOMER("Waiting for Customer"),
        RESOLVED("Resolved"),
        CLOSED("Closed"),
        REOPENED("Reopened");

        public final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum Priority {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        URGENT("Urgent");

        public final String displayName;

        Priority(String displayName) {
            this.displayName = displayName;
        }
    }

    @PrePersist
    public void generateTicketId() {
        if (this.ticketId == null) {
            this.ticketId = "TKT-" + System.currentTimeMillis();
        }
    }
}

