package com.lms.dto;

import com.lms.model.AssignmentAllocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentAllocationDTO {

    private Long id;

    private Long assignmentId;

    private String assignmentTitle;

    private Long candidateId;

    private String candidateName;

    private String candidateEmail;

    private Boolean notificationSent;

    private LocalDateTime allocatedAt;

    private LocalDateTime notificationSentAt;

    private AssignmentAllocation.AllocationStatus status;

    private String courseTitle;
}

