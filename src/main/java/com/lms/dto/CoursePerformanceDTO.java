package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoursePerformanceDTO {
    private Long courseId;
    private String title;
    private Long studentCount;
    private Double revenue;
    private Double averageRating;
}
