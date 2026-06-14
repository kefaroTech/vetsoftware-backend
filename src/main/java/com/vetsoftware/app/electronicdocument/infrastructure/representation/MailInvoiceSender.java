package com.vetsoftware.app.electronicdocument.infrastructure.representation;

import com.vetsoftware.app.electronicdocument.application.port.out.InvoiceMailPort;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envía la representación gráfica por correo (SMTP). Se desactiva con {@code vetsoftware.mail.enabled=false}
 * (útil en dev sin servidor de correo). El adjunto es el PDF.
 */
@Component
public class MailInvoiceSender implements InvoiceMailPort {

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean enabled;

    public MailInvoiceSender(JavaMailSender mailSender,
                             @Value("${vetsoftware.mail.from}") String from,
                             @Value("${vetsoftware.mail.enabled:true}") boolean enabled) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = enabled;
    }

    @Override
    public void send(String to, String cc, String subject, String htmlBody,
                     String attachmentName, byte[] attachment) {
        if (!enabled || to == null || to.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            if (cc != null && !cc.isBlank()) helper.setCc(cc);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (attachment != null) {
                helper.addAttachment(attachmentName, new ByteArrayResource(attachment));
            }
            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send invoice email: " + e.getMessage(), e);
        }
    }
}
