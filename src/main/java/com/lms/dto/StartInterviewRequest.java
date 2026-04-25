package com.lms.dto;

import lombok.Data;

@Data
public class StartInterviewRequest {
    private String topic;
    private String difficulty = "INTERMEDIATE";
    private int totalQuestions = 5;
    private String interviewType = "TEXT"; // TEXT or VOICE
}

