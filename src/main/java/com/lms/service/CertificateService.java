package com.lms.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.lms.model.Course;
import com.lms.model.Enrollment;
import com.lms.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {
    
    private final com.lms.repository.CertificateRepository certificateRepository;
    private final com.lms.repository.EnrollmentRepository enrollmentRepository;
    private final MediaService mediaService;
    
    public void issueCertificate(Long userId, Long courseId) {
        Enrollment e = enrollmentRepository.findByStudentIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        
        byte[] pdf = generateCertificate(e);
        String filename = "cert_" + e.getCourse().getId() + "_" + userId + "_" + System.currentTimeMillis() + ".pdf";
        
        try {
            // Use the new byte[] upload method
            String url = mediaService.uploadFile(pdf, filename, "application/pdf", "certificates");
            
            com.lms.model.Certificate cert = new com.lms.model.Certificate();
            cert.setUser(e.getStudent());
            cert.setCourse(e.getCourse());
            cert.setTenant(e.getTenant());
            cert.setVerificationCode("CERT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            cert.setPdfUrl(url);
            cert.setIssuedAt(java.time.LocalDateTime.now());
            certificateRepository.save(cert);
            
            log.info("Certificate issued for user {} in course {}", userId, courseId);
        } catch (Exception err) {
            log.error("Failed to issue certificate", err);
        }
    }

    public byte[] generateCertificate(Enrollment enrollment) {
        User student = enrollment.getStudent();
        Course course = enrollment.getCourse();
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Font styles (Using standard Helvetica fonts as they are built-in)
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 40, BaseColor.DARK_GRAY);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 20, BaseColor.GRAY);
            Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 45, new BaseColor(79, 70, 229)); // Indigo
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 18, BaseColor.DARK_GRAY);
            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 14, BaseColor.GRAY);

            // Add content
            Paragraph certHeader = new Paragraph("CERTIFICATE OF COMPLETION", titleFont);
            certHeader.setAlignment(Element.ALIGN_CENTER);
            certHeader.setSpacingBefore(50);
            document.add(certHeader);

            Paragraph presentedTo = new Paragraph("This is proudly presented to", subTitleFont);
            presentedTo.setAlignment(Element.ALIGN_CENTER);
            presentedTo.setSpacingBefore(30);
            document.add(presentedTo);

            String studentName = (student.getFirstName() != null ? student.getFirstName() : "") + " " + (student.getLastName() != null ? student.getLastName() : "");
            Paragraph name = new Paragraph(studentName.trim(), nameFont);
            name.setAlignment(Element.ALIGN_CENTER);
            name.setSpacingBefore(10);
            document.add(name);

            Paragraph bodyText = new Paragraph("For the successful completion of the course", bodyFont);
            bodyText.setAlignment(Element.ALIGN_CENTER);
            bodyText.setSpacingBefore(30);
            document.add(bodyText);

            String title = (course.getTitle() != null ? course.getTitle() : "Course");
            Paragraph courseTitle = new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22));
            courseTitle.setAlignment(Element.ALIGN_CENTER);
            courseTitle.setSpacingBefore(10);
            document.add(courseTitle);

            LineSeparator line = new LineSeparator();
            line.setLineColor(new BaseColor(226, 232, 240));
            line.setOffset(-10);
            document.add(new Chunk(line));

            LocalDateTime certDate = enrollment.getCompletedAt();
            if (certDate == null) certDate = enrollment.getUpdatedAt();
            if (certDate == null) certDate = LocalDateTime.now();
            
            Paragraph footer = new Paragraph("Awarded on " + certDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")), dateFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(40);
            document.add(footer);

            document.close();
            log.info("Certificate generated for user {} for course {}", student.getEmail(), course.getTitle());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating certificate: ", e);
            String msg = (e instanceof NullPointerException) ? "NPE - likely missing related data" : e.getMessage();
            throw new RuntimeException("Could not generate certificate PDF: " + msg);
        }
    }
}
