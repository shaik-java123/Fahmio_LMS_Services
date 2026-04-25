package com.lms.controller;

import com.lms.dto.SupportRequestDTO;
import com.lms.dto.SupportStatisticsDTO;
import com.lms.model.SupportRequest;
import com.lms.service.SupportRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class SupportController {

    private final SupportRequestService supportService;

    /**
     * Submit a new support request
     * POST /api/support/contact
     */
    @PostMapping("/contact")
    public ResponseEntity<SupportRequestDTO> submitSupportRequest(@RequestBody SupportRequestDTO request) {
        log.info("Received new support request from: {}", request.getEmail());

        try {
            SupportRequestDTO response = supportService.submitSupportRequest(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error submitting support request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all support requests (Admin)
     * GET /api/support/requests
     */
    @GetMapping("/requests")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<SupportRequest>> getAllRequests(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        log.debug("Fetching all support requests");

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<SupportRequest> requests = supportService.getAllSupportRequests(pageable);

        return ResponseEntity.ok(requests);
    }

    /**
     * Get support requests by status (Admin)
     * GET /api/support/requests/status/{status}
     */
    @GetMapping("/requests/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<SupportRequest>> getRequestsByStatus(
        @PathVariable String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("Fetching requests with status: {}", status);

        try {
            SupportRequest.Status requestStatus = SupportRequest.Status.valueOf(status.toUpperCase());
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<SupportRequest> requests = supportService.getSupportRequestsByStatus(requestStatus, pageable);

            return ResponseEntity.ok(requests);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get support requests by category (Admin)
     * GET /api/support/requests/category/{category}
     */
    @GetMapping("/requests/category/{category}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<SupportRequest>> getRequestsByCategory(
        @PathVariable String category,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("Fetching requests with category: {}", category);

        try {
            SupportRequest.Category requestCategory = SupportRequest.Category.valueOf(category.toUpperCase());
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<SupportRequest> requests = supportService.getSupportRequestsByCategory(requestCategory, pageable);

            return ResponseEntity.ok(requests);
        } catch (IllegalArgumentException e) {
            log.error("Invalid category: {}", category);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get support requests by email (User)
     * GET /api/support/requests/email/{email}
     */
    @GetMapping("/requests/email/{email}")
    public ResponseEntity<Page<SupportRequest>> getRequestsByEmail(
        @PathVariable String email,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("Fetching requests for email: {}", email);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SupportRequest> requests = supportService.getSupportRequestsByEmail(email, pageable);

        return ResponseEntity.ok(requests);
    }

    /**
     * Get support request by ticket ID
     * GET /api/support/requests/ticket/{ticketId}
     */
    @GetMapping("/requests/ticket/{ticketId}")
    public ResponseEntity<SupportRequestDTO> getRequestByTicketId(@PathVariable String ticketId) {
        log.debug("Fetching request with ticket ID: {}", ticketId);

        try {
            SupportRequestDTO request = supportService.getSupportRequestByTicketId(ticketId);
            return ResponseEntity.ok(request);
        } catch (RuntimeException e) {
            log.error("Support request not found: {}", ticketId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get support request by ID
     * GET /api/support/requests/{id}
     */
    @GetMapping("/requests/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getRequestById(@PathVariable Long id) {
        log.debug("Fetching request with ID: {}", id);

        // This would require implementing a new method in the service
        // For now, we'll return a 501 Not Implemented
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Update support request status (Admin)
     * PUT /api/support/requests/{id}/status
     */
    @PutMapping("/requests/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<SupportRequestDTO> updateRequestStatus(
        @PathVariable Long id,
        @RequestParam String status
    ) {
        log.info("Updating support request {} status to: {}", id, status);

        try {
            SupportRequest.Status requestStatus = SupportRequest.Status.valueOf(status.toUpperCase());
            SupportRequestDTO response = supportService.updateSupportRequestStatus(id, requestStatus);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (RuntimeException e) {
            log.error("Error updating support request status", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Add admin response to support request
     * POST /api/support/requests/{id}/response
     */
    @PostMapping("/requests/{id}/response")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<SupportRequestDTO> addResponse(
        @PathVariable Long id,
        @RequestBody Map<String, String> request
    ) {
        log.info("Adding response to support request: {}", id);

        try {
            String response = request.get("response");
            if (response == null || response.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            SupportRequestDTO result = supportService.addAdminResponse(id, response);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error adding response to support request", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Close support request
     * PUT /api/support/requests/{id}/close
     */
    @PutMapping("/requests/{id}/close")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<SupportRequestDTO> closeRequest(@PathVariable Long id) {
        log.info("Closing support request: {}", id);

        try {
            SupportRequestDTO response = supportService.closeSupportRequest(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error closing support request", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Assign support request to staff member
     * PUT /api/support/requests/{id}/assign
     */
    @PutMapping("/requests/{id}/assign")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<SupportRequestDTO> assignRequest(
        @PathVariable Long id,
        @RequestParam String staffMemberId
    ) {
        log.info("Assigning support request {} to staff member: {}", id, staffMemberId);

        try {
            SupportRequestDTO response = supportService.assignSupportRequest(id, staffMemberId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error assigning support request", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get support statistics (Admin)
     * GET /api/support/statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<SupportStatisticsDTO> getStatistics() {
        log.debug("Fetching support statistics");

        try {
            SupportStatisticsDTO statistics = supportService.getStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error fetching support statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check for support service
     * GET /api/support/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Support service is running");
    }
}

