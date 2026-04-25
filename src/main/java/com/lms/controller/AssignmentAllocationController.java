package com.lms.controller;

import com.lms.dto.AssignmentAllocationDTO;
import com.lms.dto.AssignmentAllocationRequest;
import com.lms.model.AssignmentAllocation;
import com.lms.model.User;
import com.lms.repository.UserRepository;
import com.lms.service.AssignmentAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignment-allocations")
@RequiredArgsConstructor
public class AssignmentAllocationController {

    private final AssignmentAllocationService allocationService;
    private final UserRepository userRepository;

    /**
     * Assign an assignment to multiple candidates
     * POST /api/assignment-allocations/assign
     */
    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> assignAssignmentToCandidates(
            @RequestBody AssignmentAllocationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<AssignmentAllocation> allocations =
                allocationService.allocateAssignmentToCandidates(request, user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Assignment allocated to " + allocations.size() + " candidates",
                "allocations", allocations
        ));
    }

    /**
     * Get all allocations for a specific assignment
     * GET /api/assignment-allocations/assignment/{assignmentId}
     */
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<List<AssignmentAllocationDTO>> getAllocationsForAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<AssignmentAllocationDTO> allocations =
                allocationService.getAllocationsForAssignment(assignmentId, user.getId());

        return ResponseEntity.ok(allocations);
    }

    /**
     * Get all allocations for a specific candidate
     * GET /api/assignment-allocations/candidate/{candidateId}
     */
    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<AssignmentAllocationDTO>> getAllocationsForCandidate(
            @PathVariable Long candidateId) {

        List<AssignmentAllocationDTO> allocations =
                allocationService.getAllocationsForCandidate(candidateId);

        return ResponseEntity.ok(allocations);
    }

    /**
     * Get a specific allocation
     * GET /api/assignment-allocations/{allocationId}
     */
    @GetMapping("/{allocationId}")
    public ResponseEntity<AssignmentAllocationDTO> getAllocation(@PathVariable Long allocationId) {
        AssignmentAllocationDTO allocation = allocationService.getAllocation(allocationId);
        return ResponseEntity.ok(allocation);
    }

    /**
     * Update allocation status
     * PATCH /api/assignment-allocations/{allocationId}/status
     */
    @PatchMapping("/{allocationId}/status")
    public ResponseEntity<Map<String, Object>> updateAllocationStatus(
            @PathVariable Long allocationId,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");
        AssignmentAllocation.AllocationStatus allocationStatus =
                AssignmentAllocation.AllocationStatus.valueOf(status.toUpperCase());

        AssignmentAllocation updated =
                allocationService.updateAllocationStatus(allocationId, allocationStatus);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Allocation status updated",
                "allocation", updated
        ));
    }

    /**
     * Remove an allocation
     * DELETE /api/assignment-allocations/{allocationId}
     */
    @DeleteMapping("/{allocationId}")
    public ResponseEntity<Map<String, String>> removeAllocation(
            @PathVariable Long allocationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        allocationService.removeAllocation(allocationId, user.getId());

        return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Allocation removed successfully"
        ));
    }

    /**
     * Resend notification for an allocation
     * POST /api/assignment-allocations/{allocationId}/resend-notification
     */
    @PostMapping("/{allocationId}/resend-notification")
    public ResponseEntity<Map<String, Object>> resendNotification(
            @PathVariable Long allocationId,
            @RequestBody(required = false) Map<String, String> request) {

        String customMessage = request != null ? request.get("customMessage") : null;
        allocationService.resendNotification(allocationId, customMessage);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notification sent to candidate"
        ));
    }

    /**
     * Send batch notifications for all unnotified allocations
     * POST /api/assignment-allocations/batch/send-notifications
     */
    @PostMapping("/batch/send-notifications")
    public ResponseEntity<Map<String, Object>> sendBatchNotifications() {
        allocationService.sendBatchNotifications();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Batch notifications sent successfully"
        ));
    }
}

