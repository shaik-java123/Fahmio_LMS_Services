package com.lms.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Payload when a student submits a quiz attempt */
@Getter
@Setter
public class QuizSubmitRequest {
    private Long attemptId;
    private List<AnswerRequest> answers;

    @Getter
    @Setter
    public static class AnswerRequest {
        private Long questionId;
        /** For MCQ / TRUE_FALSE */
        private Long selectedOptionId;
        /** For MSQ */
        private List<Long> selectedOptionIds;
        /** For SHORT_ANSWER / FILL_BLANK */
        private String textAnswer;
    }
}
