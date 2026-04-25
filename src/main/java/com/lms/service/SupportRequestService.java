package com.lms.service;

import com.lms.dto.SupportRequestDTO;
import com.lms.dto.SupportStatisticsDTO;
import com.lms.model.SupportRequest;
import com.lms.repository.SupportRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportRequestService {

    private final SupportRequestRepository supportRequestRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:support@fahmio.com}")
    private String supportEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Submit a new support request
     */
    @Transactional
    public SupportRequestDTO submitSupportRequest(SupportRequestDTO requestDTO) {
        log.info("Creating new support request from: {}", requestDTO.getEmail());

        SupportRequest request = new SupportRequest();
        request.setName(requestDTO.getName());
        request.setEmail(requestDTO.getEmail());
        request.setPhone(requestDTO.getPhone());
        request.setCategory(SupportRequest.Category.valueOf(requestDTO.getCategory()));
        request.setSubject(requestDTO.getSubject());
        request.setMessage(requestDTO.getMessage());
        request.setStatus(SupportRequest.Status.OPEN);
        request.setPriority(determinePriority(requestDTO.getCategory()));

        SupportRequest saved = supportRequestRepository.save(request);
        log.info("Support request created with ticket ID: {}", saved.getTicketId());

        // Send confirmation email asynchronously
        sendConfirmationEmail(saved);

        return SupportRequestDTO.fromEntity(saved);
    }

    /**
     * Get all support requests (Admin)
     */
    @Transactional(readOnly = true)
    public Page<SupportRequest> getAllSupportRequests(Pageable pageable) {
        log.debug("Fetching all support requests");
        return supportRequestRepository.findAll(pageable);
    }

    /**
     * Get support requests by status (Admin)
     */
    @Transactional(readOnly = true)
    public Page<SupportRequest> getSupportRequestsByStatus(SupportRequest.Status status, Pageable pageable) {
        log.debug("Fetching support requests with status: {}", status);
        return supportRequestRepository.findByStatus(status, pageable);
    }

    /**
     * Get support requests by category (Admin)
     */
    @Transactional(readOnly = true)
    public Page<SupportRequest> getSupportRequestsByCategory(SupportRequest.Category category, Pageable pageable) {
        log.debug("Fetching support requests with category: {}", category);
        return supportRequestRepository.findByCategory(category, pageable);
    }

    /**
     * Get support requests by email (User)
     */
    @Transactional(readOnly = true)
    public Page<SupportRequest> getSupportRequestsByEmail(String email, Pageable pageable) {
        log.debug("Fetching support requests for email: {}", email);
        return supportRequestRepository.findByEmail(email, pageable);
    }

    /**
     * Get support request by ticket ID
     */
    @Transactional(readOnly = true)
    public SupportRequestDTO getSupportRequestByTicketId(String ticketId) {
        log.debug("Fetching support request with ticket ID: {}", ticketId);
        SupportRequest request = supportRequestRepository.findByTicketId(ticketId)
            .orElseThrow(() -> new RuntimeException("Support request not found: " + ticketId));
        return SupportRequestDTO.fromEntity(request);
    }

    /**
     * Update support request status (Admin)
     */
    @Transactional
    public SupportRequestDTO updateSupportRequestStatus(Long id, SupportRequest.Status status) {
        log.info("Updating support request {} status to: {}", id, status);

        SupportRequest request = supportRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Support request not found: " + id));

        request.setStatus(status);
        SupportRequest updated = supportRequestRepository.save(request);

        return SupportRequestDTO.fromEntity(updated);
    }

    /**
     * Add admin response to support request
     */
    @Transactional
    public SupportRequestDTO addAdminResponse(Long id, String response) {
        log.info("Adding response to support request: {}", id);

        SupportRequest request = supportRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Support request not found: " + id));

        request.setAdminResponse(response);
        request.setRespondedAt(LocalDateTime.now());
        request.setStatus(SupportRequest.Status.IN_PROGRESS);

        SupportRequest updated = supportRequestRepository.save(request);

        // Send response email
        sendResponseEmail(updated);

        return SupportRequestDTO.fromEntity(updated);
    }

    /**
     * Close support request
     */
    @Transactional
    public SupportRequestDTO closeSupportRequest(Long id) {
        log.info("Closing support request: {}", id);

        SupportRequest request = supportRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Support request not found: " + id));

        request.setStatus(SupportRequest.Status.CLOSED);
        SupportRequest updated = supportRequestRepository.save(request);

        return SupportRequestDTO.fromEntity(updated);
    }

    /**
     * Assign support request to staff member
     */
    @Transactional
    public SupportRequestDTO assignSupportRequest(Long id, String staffMemberId) {
        log.info("Assigning support request {} to staff member: {}", id, staffMemberId);

        SupportRequest request = supportRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Support request not found: " + id));

        request.setAssignedTo(staffMemberId);
        SupportRequest updated = supportRequestRepository.save(request);

        return SupportRequestDTO.fromEntity(updated);
    }

    /**
     * Get support statistics
     */
    @Transactional(readOnly = true)
    public SupportStatisticsDTO getStatistics() {
        log.debug("Calculating support statistics");

        return SupportStatisticsDTO.builder()
            .totalRequests(supportRequestRepository.count())
            .openRequests(supportRequestRepository.countByStatus(SupportRequest.Status.OPEN))
            .inProgressRequests(supportRequestRepository.countByStatus(SupportRequest.Status.IN_PROGRESS))
            .resolvedRequests(supportRequestRepository.countByStatus(SupportRequest.Status.RESOLVED))
            .closedRequests(supportRequestRepository.countByStatus(SupportRequest.Status.CLOSED))
            .urgentRequests(supportRequestRepository.countByPriority(SupportRequest.Priority.URGENT))
            .highPriorityRequests(supportRequestRepository.countByPriority(SupportRequest.Priority.HIGH))
            .build();
    }

    /**
     * Send confirmation email to user
     */
    @Async
    protected void sendConfirmationEmail(SupportRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(request.getEmail());
            helper.setFrom(supportEmail);
            helper.setSubject("Support Request Received - Ticket #" + request.getTicketId());

            String htmlContent = buildConfirmationEmailHtml(request);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            request.setEmailSent(true);
            supportRequestRepository.save(request);

            log.info("Confirmation email sent for ticket: {}", request.getTicketId());
        } catch (MessagingException e) {
            log.error("Failed to send confirmation email for ticket: {}", request.getTicketId(), e);
        }
    }

    /**
     * Send response email to user
     */
    @Async
    protected void sendResponseEmail(SupportRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(request.getEmail());
            helper.setFrom(supportEmail);
            helper.setSubject("Response to Your Support Request - Ticket #" + request.getTicketId());

            String htmlContent = buildResponseEmailHtml(request);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Response email sent for ticket: {}", request.getTicketId());
        } catch (MessagingException e) {
            log.error("Failed to send response email for ticket: {}", request.getTicketId(), e);
        }
    }

    /**
     * Determine priority based on category
     */
    private SupportRequest.Priority determinePriority(String category) {
        return switch (category) {
            case "TECHNICAL", "BILLING" -> SupportRequest.Priority.HIGH;
            case "GENERAL", "COURSE", "FEATURE" -> SupportRequest.Priority.MEDIUM;
            case "ACCOUNT" -> SupportRequest.Priority.HIGH;
            default -> SupportRequest.Priority.MEDIUM;
        };
    }

    /**
     * Build confirmation email HTML
     */
    private String buildConfirmationEmailHtml(SupportRequest request) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(to right, #4f46e5, #7c3aed); color: white; padding: 30px; border-radius: 10px; text-align: center; margin-bottom: 30px;">
                        <h1 style="margin: 0; font-size: 28px;">Thank You for Contacting Us!</h1>
                        <p style="margin: 10px 0 0; opacity: 0.9;">Your support request has been received</p>
                    </div>
                    
                    <div style="background: #f5f5f5; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
                        <p style="margin: 0 0 15px; font-size: 16px;"><strong>Ticket ID: %s</strong></p>
                        <p style="margin: 0 0 15px;"><strong>Category:</strong> %s</p>
                        <p style="margin: 0 0 15px;"><strong>Subject:</strong> %s</p>
                    </div>
                    
                    <div style="margin-bottom: 20px; line-height: 1.6;">
                        <p>Hi %s,</p>
                        <p>We have received your support request and will review it shortly. Our team typically responds within 24 hours.</p>
                        <p>Your ticket ID is: <strong>%s</strong></p>
                        <p>Please keep this ticket ID for your records as it will help us track your request quickly.</p>
                    </div>
                    
                    <div style="background: #f5f5f5; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
                        <p style="margin: 0 0 15px;"><strong>Request Details:</strong></p>
                        <p style="margin: 0;">%s</p>
                    </div>
                    
                    <div style="border-top: 1px solid #ddd; padding-top: 20px; font-size: 12px; color: #666;">
                        <p style="margin: 0 0 10px;">Need immediate help? Contact us:</p>
                        <p style="margin: 0;">Email: support@fahmio.com</p>
                        <p style="margin: 0;">Hours: 24/7 Support Available</p>
                    </div>
                    
                    <div style="text-align: center; margin-top: 30px; font-size: 12px; color: #999;">
                        <p style="margin: 0;">© 2026 Fahmio. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                request.getTicketId(),
                request.getCategory().displayName,
                request.getSubject(),
                request.getName(),
                request.getTicketId(),
                request.getMessage()
            );
    }

    /**
     * Build response email HTML
     */
    private String buildResponseEmailHtml(SupportRequest request) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(to right, #4f46e5, #7c3aed); color: white; padding: 30px; border-radius: 10px; text-align: center; margin-bottom: 30px;">
                        <h1 style="margin: 0; font-size: 28px;">We've Responded to Your Request</h1>
                        <p style="margin: 10px 0 0; opacity: 0.9;">Ticket #%s</p>
                    </div>
                    
                    <div style="margin-bottom: 20px; line-height: 1.6;">
                        <p>Hi %s,</p>
                        <p>Thank you for your patience. Our support team has reviewed your request and provided a response below:</p>
                    </div>
                    
                    <div style="background: #f5f5f5; padding: 20px; border-left: 4px solid #4f46e5; border-radius: 8px; margin-bottom: 20px;">
                        <p style="margin: 0; white-space: pre-wrap;">%s</p>
                    </div>
                    
                    <div style="background: #e8f5e9; padding: 15px; border-radius: 8px; margin-bottom: 20px;">
                        <p style="margin: 0;"><strong>Your Original Request:</strong></p>
                        <p style="margin: 10px 0 0; font-size: 14px;">%s</p>
                    </div>
                    
                    <div style="margin-bottom: 20px;">
                        <p>If you need further assistance, please reply to this email or contact us at support@fahmio.com</p>
                    </div>
                    
                    <div style="border-top: 1px solid #ddd; padding-top: 20px; font-size: 12px; color: #666;">
                        <p style="margin: 0 0 10px;">Contact Information:</p>
                        <p style="margin: 0;">Email: support@fahmio.com</p>
                        <p style="margin: 0;">Hours: 24/7 Support Available</p>
                    </div>
                    
                    <div style="text-align: center; margin-top: 30px; font-size: 12px; color: #999;">
                        <p style="margin: 0;">© 2026 Fahmio. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                request.getTicketId(),
                request.getName(),
                request.getAdminResponse(),
                request.getMessage()
            );
    }
}

