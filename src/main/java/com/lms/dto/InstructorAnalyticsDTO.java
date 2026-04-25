package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorAnalyticsDTO {
    private Double totalRevenue;
    private Long totalStudents;
    private Long totalCourses;
    private Long totalEnrollments;
    private List<CoursePerformanceDTO> coursePerformance;
    private Map<String, Double> monthlyRevenue; // Month-Year string -> Revenue
}
