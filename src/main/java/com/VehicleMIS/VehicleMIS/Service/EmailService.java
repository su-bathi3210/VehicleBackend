package com.VehicleMIS.VehicleMIS.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${approver.email:enukaenubro@gmail.com}")
    private String APPROVER_EMAIL;

    @Value("${admin.email:ksuba3210@gmail.com}")
    private String ADMIN_EMAIL;

    public void sendApprovalEmail(String requestId) {
        String subject = "🚗 Vehicle Request Requires Your Approval";
        String message = "A new vehicle request (" + requestId + ") has been assigned and requires your approval.";
        sendEmail(APPROVER_EMAIL, subject, message);
    }

    public void sendAdminEmail(String message) {
        String subject = "🚗 Vehicle Request Update";
        sendEmail(ADMIN_EMAIL, subject, message);
    }

    private void sendEmail(String to, String subject, String body) {
        if (mailSender == null) {
            logger.info("Email not sent (no mailSender). To: {}, Subject: {}, Body: {}", to, subject, body);
            return;
        }

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(body);
        mailSender.send(mailMessage);
        logger.info("Email sent to {}", to);
    }
}
