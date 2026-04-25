package com.lms.dto;

import lombok.Data;

@Data
public class GradeSubmissionRequest {
    private Integer grade;
    private String feedback;
}
