package com.lms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_classes")
@Getter
@Setter
@NoArgsConstructor
public class LiveClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private Integer durationMinutes;

    @Column(nullable = false)
    private String meetingUrl; // Zoom or Google Meet link

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private ClassStatus status = ClassStatus.UPCOMING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum ClassStatus {
        UPCOMING, LIVE, COMPLETED, CANCELLED
    }
}
