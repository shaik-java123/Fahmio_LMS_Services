package com.lms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(length = 2000)
    private String content;

    private String fileUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime submittedAt;

    private Integer grade;

    @Column(length = 1000)
    private String feedback;

    private Double plagiarismScore; // 0.0 - 1.0
    
    private Integer fileSizeKb;

    private LocalDateTime gradedAt;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    private Status status = Status.SUBMITTED;

    public enum Status {
        SUBMITTED,
        GRADED,
        LATE
    }
}
