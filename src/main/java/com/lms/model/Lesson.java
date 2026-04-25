package com.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private ContentType contentType = ContentType.TEXT;

    private String videoUrl;
    private String audioUrl;

    private String documentUrl;
    private String externalUrl;

    /** For Live Classes (Zoom/GMeet) */
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    private Integer orderIndex;

    @Column(nullable = false)
    private Integer durationMinutes;

    /** Points awarded to the student upon completing this lesson */
    @Column(columnDefinition = "int default 0")
    private Integer learningPoints = 0;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "lesson_attachments", joinColumns = @JoinColumn(name = "lesson_id"))
    @Column(name = "attachment_url")
    private java.util.Set<String> attachments = new java.util.HashSet<>();

    @ElementCollection
    @CollectionTable(name = "lesson_prerequisites", joinColumns = @JoinColumn(name = "lesson_id"))
    @Column(name = "prerequisite_id")
    private java.util.Set<Long> prerequisites = new java.util.HashSet<>();

    public enum ContentType {
        TEXT,
        VIDEO,
        AUDIO,
        DOCUMENT,
        PDF,
        QUIZ,
        LINK,
        SCORM,
        EMBED,
        LIVE_CLASS
    }
}
