package com.lms.dto;

import com.lms.model.SupportRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportRequestDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String category;
    private String subject;
    private String message;
    private String ticketId;
    private String status;
    private String priority;
    private String assignedTo;
    private String adminResponse;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SupportRequestDTO fromEntity(SupportRequest request) {
        return SupportRequestDTO.builder()
            .id(request.getId())
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .category(request.getCategory().name())
            .subject(request.getSubject())
            .message(request.getMessage())
            .ticketId(request.getTicketId())
            .status(request.getStatus().name())
            .priority(request.getPriority().name())
            .assignedTo(request.getAssignedTo())
            .adminResponse(request.getAdminResponse())
            .respondedAt(request.getRespondedAt())
            .createdAt(request.getCreatedAt())
            .updatedAt(request.getUpdatedAt())
            .build();
    }

    public SupportRequest toEntity() {
        SupportRequest request = new SupportRequest();
        request.setName(this.name);
        request.setEmail(this.email);
        request.setPhone(this.phone);
        request.setCategory(SupportRequest.Category.valueOf(this.category));
        request.setSubject(this.subject);
        request.setMessage(this.message);
        return request;
    }
}

