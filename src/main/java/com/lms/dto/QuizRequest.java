package com.lms.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Payload for creating or updating a quiz */
@Getter
@Setter
public class QuizRequest {
    private Long courseId;
    private Long lessonId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Integer passingScore;
    private Integer attemptsAllowed;
    private boolean shuffleQuestions;
    private List<QuestionRequest> questions;

    @Getter
    @Setter
    public static class QuestionRequest {
        private String type;  // MCQ, MSQ, TRUE_FALSE, SHORT_ANSWER, FILL_BLANK
        private String text;
        private String explanation;
        private Integer points;
        private Integer orderIndex;
        private List<OptionRequest> options;
    }

    @Getter
    @Setter
    public static class OptionRequest {
        private String text;
        private boolean correct;
    }
}
