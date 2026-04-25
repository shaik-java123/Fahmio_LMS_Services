package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentAllocationRequest {

    private Long assignmentId;

    private List<Long> candidateIds;

    private String customMessage; // Optional custom message to include in notification

    private Boolean sendImmediately = true; // Whether to send notification immediately
}

