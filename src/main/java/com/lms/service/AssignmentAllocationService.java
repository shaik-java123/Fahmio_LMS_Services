package com.lms.service;

import com.lms.dto.AssignmentAllocationDTO;
import com.lms.dto.AssignmentAllocationRequest;
import com.lms.model.Assignment;
import com.lms.model.AssignmentAllocation;
import com.lms.model.Notification;
import com.lms.model.User;
import com.lms.repository.AssignmentAllocationRepository;
import com.lms.repository.AssignmentRepository;
import com.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentAllocationService {

    private final AssignmentAllocationRepository allocationRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    /**
     * Assign an assignment to multiple candidates
     */
    @Transactional
    public List<AssignmentAllocation> allocateAssignmentToCandidates(
            AssignmentAllocationRequest request,
            Long instructorId) {

        log.info("Starting assignment allocation for assignment {} to {} candidates",
                 request.getAssignmentId(), request.getCandidateIds().size());

        // Validate assignment exists
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // Validate instructor owns the assignment
        if (!assignment.getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to allocate this assignment");
        }

        List<AssignmentAllocation> allocations = request.getCandidateIds().stream()
                .map(candidateId -> allocateToSingleCandidate(assignment, candidateId, request))
                .collect(Collectors.toList());

        log.info("Successfully allocated assignment to {} candidates", allocations.size());
        return allocations;
    }

    /**
     * Allocate assignment to a single candidate
     */
    @Transactional
    private AssignmentAllocation allocateToSingleCandidate(
            Assignment assignment,
            Long candidateId,
            AssignmentAllocationRequest request) {

        // Check if candidate exists
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found with ID: " + candidateId));

        // Check if already allocated
        if (allocationRepository.findByAssignmentIdAndCandidateId(
                assignment.getId(), candidateId).isPresent()) {
            log.warn("Assignment {} already allocated to candidate {}",
                     assignment.getId(), candidateId);
            throw new RuntimeException("Assignment already allocated to this candidate");
        }

        // Create allocation
        AssignmentAllocation allocation = new AssignmentAllocation();
        allocation.setAssignment(assignment);
        allocation.setCandidate(candidate);
        allocation.setNotificationSent(false);
        allocation.setStatus(AssignmentAllocation.AllocationStatus.ASSIGNED);

        AssignmentAllocation savedAllocation = allocationRepository.save(allocation);

        // Send notification immediately if requested
        if (Boolean.TRUE.equals(request.getSendImmediately())) {
            sendAllocationNotification(savedAllocation, request.getCustomMessage());
        }

        return savedAllocation;
    }

    /**
     * Send notification to candidate about assignment allocation
     */
    @Transactional
    public void sendAllocationNotification(AssignmentAllocation allocation, String customMessage) {
        try {
            User candidate = allocation.getCandidate();
            Assignment assignment = allocation.getAssignment();

            // Create notification title and message
            String title = "New Assignment: " + assignment.getTitle();
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("You have been assigned a new assignment: ")
                    .append(assignment.getTitle())
                    .append(" in course ")
                    .append(assignment.getCourse().getTitle());

            if (assignment.getDueDate() != null) {
                messageBuilder.append(". Due date: ").append(assignment.getDueDate());
            }

            if (customMessage != null && !customMessage.isEmpty()) {
                messageBuilder.append("\n\n").append(customMessage);
            }

            String message = messageBuilder.toString();

            // Create in-app notification
            notificationService.createNotification(
                    candidate.getId(),
                    title,
                    message,
                    Notification.Type.ASSIGNMENT
            );

            // Send email notification
            try {
                emailService.sendAssignmentAllocationNotification(
                        candidate.getEmail(),
                        candidate.getFirstName(),
                        assignment.getTitle(),
                        assignment.getCourse().getTitle(),
                        assignment.getDueDate(),
                        assignment.getDescription(),
                        customMessage
                );
            } catch (Exception e) {
                log.warn("Failed to send email notification to {}: {}",
                         candidate.getEmail(), e.getMessage());
            }

            // Update allocation
            allocation.setNotificationSent(true);
            allocation.setNotificationSentAt(LocalDateTime.now());
            allocationRepository.save(allocation);

            log.info("Notification sent to candidate {} for assignment {}",
                     candidate.getId(), assignment.getId());

        } catch (Exception e) {
            log.error("Error sending allocation notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send notification: " + e.getMessage());
        }
    }

    /**
     * Get all allocations for a specific assignment
     */
    @Transactional(readOnly = true)
    public List<AssignmentAllocationDTO> getAllocationsForAssignment(Long assignmentId, Long instructorId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (!assignment.getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to view these allocations");
        }

        return allocationRepository.findByAssignmentId(assignmentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all allocations for a specific candidate
     */
    @Transactional(readOnly = true)
    public List<AssignmentAllocationDTO> getAllocationsForCandidate(Long candidateId) {
        return allocationRepository.findByCandidateId(candidateId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific allocation
     */
    @Transactional(readOnly = true)
    public AssignmentAllocationDTO getAllocation(Long allocationId) {
        AssignmentAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));
        return convertToDTO(allocation);
    }

    /**
     * Update allocation status
     */
    @Transactional
    public AssignmentAllocation updateAllocationStatus(
            Long allocationId,
            AssignmentAllocation.AllocationStatus status) {

        AssignmentAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));

        allocation.setStatus(status);
        return allocationRepository.save(allocation);
    }

    /**
     * Remove an allocation
     */
    @Transactional
    public void removeAllocation(Long allocationId, Long instructorId) {
        AssignmentAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));

        // Verify instructor permission
        if (!allocation.getAssignment().getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to remove this allocation");
        }

        allocationRepository.delete(allocation);
        log.info("Allocation {} removed by instructor {}", allocationId, instructorId);
    }

    /**
     * Resend notification for an allocation
     */
    @Transactional
    public void resendNotification(Long allocationId, String customMessage) {
        AssignmentAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));

        sendAllocationNotification(allocation, customMessage);
    }

    /**
     * Get unnotified allocations (for batch processing)
     */
    @Transactional(readOnly = true)
    public List<AssignmentAllocation> getUnnotifiedAllocations() {
        return allocationRepository.findUnnotifiedAllocations();
    }

    /**
     * Send batch notifications for all unnotified allocations
     */
    @Transactional
    public void sendBatchNotifications() {
        List<AssignmentAllocation> unnotified = getUnnotifiedAllocations();
        log.info("Processing {} unnotified allocations", unnotified.size());

        for (AssignmentAllocation allocation : unnotified) {
            try {
                sendAllocationNotification(allocation, null);
            } catch (Exception e) {
                log.error("Failed to send notification for allocation {}: {}",
                         allocation.getId(), e.getMessage());
            }
        }
    }

    /**
     * Convert AssignmentAllocation to DTO
     */
    private AssignmentAllocationDTO convertToDTO(AssignmentAllocation allocation) {
        return AssignmentAllocationDTO.builder()
                .id(allocation.getId())
                .assignmentId(allocation.getAssignment().getId())
                .assignmentTitle(allocation.getAssignment().getTitle())
                .candidateId(allocation.getCandidate().getId())
                .candidateName(allocation.getCandidate().getFirstName() + " " +
                              allocation.getCandidate().getLastName())
                .candidateEmail(allocation.getCandidate().getEmail())
                .notificationSent(allocation.getNotificationSent())
                .allocatedAt(allocation.getAllocatedAt())
                .notificationSentAt(allocation.getNotificationSentAt())
                .status(allocation.getStatus())
                .courseTitle(allocation.getAssignment().getCourse().getTitle())
                .build();
    }
}

