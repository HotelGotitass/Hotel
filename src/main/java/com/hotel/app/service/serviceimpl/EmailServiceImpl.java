package com.hotel.app.service.serviceimpl;

import com.hotel.app.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendSimpleEmail(String to, String subject, String text) {
        executeAfterCommitOrImmediate(() -> dispatchSimpleEmail(to, subject, text));
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        executeAfterCommitOrImmediate(() -> dispatchHtmlEmail(to, subject, htmlContent));
    }

    private void executeAfterCommitOrImmediate(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        task.run();
                    } catch (Exception e) {
                        log.error("Fallo al enviar correo tras commit: {}", e.getMessage(), e);
                    }
                }
            });
        } else {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Fallo al enviar correo síncrono: {}", e.getMessage(), e);
            }
        }
    }

    private void dispatchSimpleEmail(String to, String subject, String text) {
        if (!isMailConfigured()) {
            log.warn("Correo no enviado: credenciales SMTP no configuradas en el entorno.");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Notificación por correo simple despachada exitosamente a {}.", to);
        } catch (Exception e) {
            log.error("Fallo al enviar correo simple a {}. Error: {}", to, e.getMessage(), e);
        }
    }

    private void dispatchHtmlEmail(String to, String subject, String htmlContent) {
        if (!isMailConfigured()) {
            log.warn("Correo no enviado: credenciales SMTP no configuradas en el entorno.");
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Notificación por correo HTML despachada exitosamente a {}.", to);
        } catch (Exception e) {
            log.error("Fallo al enviar correo HTML a {}. Error: {}", to, e.getMessage(), e);
        }
    }

    private boolean isMailConfigured() {
        return fromEmail != null && !fromEmail.isBlank() && mailPassword != null && !mailPassword.isBlank();
    }
}
