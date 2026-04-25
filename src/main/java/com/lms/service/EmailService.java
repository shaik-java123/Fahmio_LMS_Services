package com.lms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        String subject = "Verify your email address";
        String link = frontendUrl + "/verify-email?token=" + token;
        String html = buildEmailHtml(firstName,
                "Welcome to LearnHub! Please verify your email.",
                "Verify Email",
                link,
                "This link expires in 24 hours.");
        sendEmail(toEmail, subject, html);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String token) {
        String subject = "Reset your password";
        String link = frontendUrl + "/reset-password?token=" + token;
        String html = buildEmailHtml(firstName,
                "You requested a password reset.",
                "Reset Password",
                link,
                "This link expires in 1 hour. If you didn't request this, ignore this email.");
        sendEmail(toEmail, subject, html);
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String firstName) {
        String subject = "Welcome to LearnHub!";
        String html = buildEmailHtml(firstName,
                "Your account is ready. Start learning today!",
                "Browse Courses",
                frontendUrl + "/courses",
                "Happy learning!");
        sendEmail(toEmail, subject, html);
    }

    @Async
    public void sendEnrollmentConfirmation(String toEmail, String firstName, String courseTitle) {
        String subject = "You're enrolled: " + courseTitle;
        String html = buildEmailHtml(firstName,
                "You've successfully enrolled in <strong>" + courseTitle + "</strong>.",
                "Start Learning",
                frontendUrl + "/my-learning",
                "Good luck on your learning journey!");
        sendEmail(toEmail, subject, html);
    }

    @Async
    public void sendCertificateIssuedEmail(String toEmail, String firstName,
                                            String courseTitle, String verificationCode) {
        String subject = "🎉 Your certificate is ready: " + courseTitle;
        String verifyUrl = frontendUrl + "/certificate/" + verificationCode + "/verify";
        String html = buildEmailHtml(firstName,
                "Congratulations! You've completed <strong>" + courseTitle + "</strong>. " +
                "Your certificate verification code is: <strong>" + verificationCode + "</strong>",
                "View Certificate",
                verifyUrl,
                "Share this certificate to showcase your achievement.");
        sendEmail(toEmail, subject, html);
    }

    @Async
    public void sendReviewNotification(String toEmail, String instructorName, String studentName, String courseTitle, int rating) {
        String stars = "⭐".repeat(rating);
        String subject = "New review for " + courseTitle;
        String html = buildEmailHtml(instructorName,
                "<strong>" + studentName + "</strong> just left a " + rating + "-star review for your course: " + courseTitle + " " + stars,
                "View Reviews",
                frontendUrl + "/instructor/courses",
                "Keep up the great work!");
        sendEmail(toEmail, subject, html);
    }

    @Async
    public void sendAssignmentSubmissionNotification(String toEmail, String instructorName, String studentName, String assignmentTitle, String courseTitle) {
        String subject = "New assignment submission: " + assignmentTitle;
        String html = buildEmailHtml(instructorName,
                "<strong>" + studentName + "</strong> just submitted their response for <strong>" + assignmentTitle + "</strong> in your course: " + courseTitle,
                "Review Submission",
                frontendUrl + "/instructor/submissions", // Hypothetical route
                "Keep guiding your students!");
        sendEmail(toEmail, subject, html);
    }

    @Async
    public void sendCertificateSharedEmail(String recipientEmail, String recipientName,
                                            String senderName, String courseTitle,
                                            String personalMessage, String verifyUrl) {
        String subject = senderName + " shared a certificate with you!";
        String messageBody = "<strong>" + senderName + "</strong> has shared their certificate of completion for " +
                "<strong>" + courseTitle + "</strong> with you." +
                (personalMessage != null && !personalMessage.isBlank()
                        ? "<br><br><em>\"" + personalMessage + "\"</em>"
                        : "") +
                "<br><br>Click the button below to view and verify this credential.";
        String html = buildEmailHtml(recipientName, messageBody, "View Certificate", verifyUrl,
                "This certificate has been digitally signed and is verifiable online.");
        sendEmail(recipientEmail, subject, html);
    }

    @Async
    public void sendGradeNotification(String toEmail, String studentName, String assignmentTitle, Integer grade, String feedback) {
        String subject = "Assignment Graded: " + assignmentTitle;
        String html = buildEmailHtml(studentName,
                "Your submission for <strong>" + assignmentTitle + "</strong> has been graded. Your score is: <strong>" + grade + "/100</strong>.<br><br><strong>Instructor Feedback:</strong> " + feedback,
                "View Result",
                frontendUrl + "/my-learning",
                "Keep working hard!");
        sendEmail(toEmail, subject, html);
    }

    @Async
    public void sendAssignmentAllocationNotification(String toEmail, String candidateName,
                                                      String assignmentTitle, String courseTitle,
                                                      java.time.LocalDateTime dueDate,
                                                      String assignmentDescription, String customMessage) {
        String subject = "New Assignment Assigned: " + assignmentTitle;
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("You have been assigned a new assignment: <strong>")
                .append(assignmentTitle).append("</strong> in the course <strong>")
                .append(courseTitle).append("</strong>.");

        if (assignmentDescription != null && !assignmentDescription.isEmpty()) {
            messageBuilder.append("<br><br><strong>Description:</strong> ")
                    .append(assignmentDescription);
        }

        if (dueDate != null) {
            messageBuilder.append("<br><br><strong>Due Date:</strong> ")
                    .append(dueDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm")));
        }

        if (customMessage != null && !customMessage.isEmpty()) {
            messageBuilder.append("<br><br><strong>Instructor Message:</strong> ")
                    .append(customMessage);
        }

        String html = buildEmailHtml(candidateName,
                messageBuilder.toString(),
                "View Assignment",
                frontendUrl + "/my-learning",
                "Submit your assignment on time to get the best feedback!");
        sendEmail(toEmail, subject, html);
    }


    private void sendEmail(String to, String subject, String htmlContent) {
        if (fromEmail == null || fromEmail.contains("your-email@")) {
            log.warn("Mock email mode (Dev) -> To: {}, Subject: {}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildEmailHtml(String name, String message, String btnText,
                                   String btnUrl, String footer) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: 'Inter', Arial, sans-serif; background: #f8fafc; margin: 0; padding: 40px 20px;">
              <div style="max-width: 560px; margin: 0 auto; background: #fff; border-radius: 12px;
                          box-shadow: 0 4px 24px rgba(0,0,0,0.08); overflow: hidden;">
                <div style="background: linear-gradient(135deg, #0284c7, #0ea5e9); padding: 32px; text-align: center;">
                  <h1 style="color: #fff; margin: 0; font-size: 24px; font-weight: 700;">LearnHub</h1>
                  <p style="color: rgba(255,255,255,0.85); margin: 8px 0 0; font-size: 14px;">Your Learning Platform</p>
                </div>
                <div style="padding: 40px 32px;">
                  <p style="font-size: 16px; color: #374151; margin: 0 0 12px;">Hi %s,</p>
                  <p style="font-size: 16px; color: #374151; margin: 0 0 32px;">%s</p>
                  <div style="text-align: center; margin: 32px 0;">
                    <a href="%s" style="background: #0284c7; color: #fff; padding: 14px 32px; border-radius: 8px;
                       text-decoration: none; font-weight: 600; font-size: 16px; display: inline-block;">%s</a>
                  </div>
                  <p style="font-size: 13px; color: #9ca3af; margin: 24px 0 0;">%s</p>
                </div>
                <div style="background: #f8fafc; padding: 20px 32px; text-align: center;
                            border-top: 1px solid #e5e7eb;">
                  <p style="font-size: 12px; color: #9ca3af; margin: 0;">
                    © 2026 LearnHub. All rights reserved.
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(name, message, btnUrl, btnText, footer);
    }
}
