package com.lms.controller;

import com.lms.dto.ShareCertificateRequest;
import com.lms.model.Certificate;
import com.lms.model.Enrollment;
import com.lms.repository.CertificateRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.security.JwtUtil;
import com.lms.service.CertificateService;
import com.lms.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class CertificateController {

    private final EnrollmentRepository enrollmentRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateService certificateService;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/my")
    public ResponseEntity<List<Certificate>> getMyCertificates(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(certificateRepository.findByUserId(userId));
    }

    @GetMapping("/verify/{code}")
    public ResponseEntity<Certificate> verifyCertificate(@PathVariable String code) {
        return certificateRepository.findByVerificationCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/share/{id}")
    public ResponseEntity<Void> shareCertificate(
            @PathVariable Long id,
            @RequestBody ShareCertificateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));

        String verifyUrl = frontendUrl + "/certificate/" + cert.getVerificationCode() + "/verify";
        String senderName = (cert.getUser().getFirstName() + " " + cert.getUser().getLastName()).trim();
        String courseTitle = cert.getCourse().getTitle();

        emailService.sendCertificateSharedEmail(
                request.getRecipientEmail(),
                request.getRecipientName() != null ? request.getRecipientName() : "there",
                senderName,
                courseTitle,
                request.getPersonalMessage(),
                verifyUrl
        );

        log.info("Certificate {} shared by {} to {}", id, senderName, request.getRecipientEmail());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/download/{courseId}")
    public ResponseEntity<ByteArrayResource> downloadCertificate(@PathVariable Long courseId, @RequestParam Long userId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        return generateDownloadResponse(enrollment, courseId);
    }

    @GetMapping("/download/id/{id}")
    public ResponseEntity<ByteArrayResource> downloadCertificateById(@PathVariable Long id) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(cert.getUser().getId(), cert.getCourse().getId())
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        return generateDownloadResponse(enrollment, cert.getCourse().getId());
    }

    private ResponseEntity<ByteArrayResource> generateDownloadResponse(Enrollment enrollment, Long courseId) {
        if (enrollment.getProgress() < 100) {
            log.warn("User {} path to certificate blocked. Progress: {}%", enrollment.getStudent().getId(), enrollment.getProgress());
            throw new RuntimeException("Course must be completed to download a certificate");
        }

        byte[] pdfContent = certificateService.generateCertificate(enrollment);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Certificate_" + courseId + ".pdf");
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(pdfContent.length)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdfContent));
    }
}
