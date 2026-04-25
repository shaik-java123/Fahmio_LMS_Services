package com.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mock_interviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MockInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** e.g. "Java", "React", "System Design", "Data Structures" */
    @Column(nullable = false)
    private String topic;

    /** BEGINNER / INTERMEDIATE / ADVANCED */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    private Difficulty difficulty = Difficulty.INTERMEDIATE;

    /** TEXT or VOICE based interview */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private InterviewType interviewType = InterviewType.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    private Status status = Status.IN_PROGRESS;

    /** 0-100 overall score assigned after completion */
    private Integer overallScore;

    /** AI-generated overall feedback paragraph */
    @Column(length = 4000)
    private String overallFeedback;

    /** Total questions in this session */
    private Integer totalQuestions = 5;

    @CreationTimestamp
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @JsonIgnoreProperties({"interview"})
    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<InterviewQuestion> questions = new ArrayList<>();

    public enum Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }
    public enum Status { IN_PROGRESS, COMPLETED, ABANDONED }
    public enum InterviewType { TEXT, VOICE }
}
