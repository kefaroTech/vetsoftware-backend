package com.vetsoftware.app.registration.infrastructure.email;

import com.vetsoftware.app.registration.application.port.out.VerificationEmailSender;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envia por SMTP el correo de verificacion de cuenta. El enlace apunta a la pagina del front
 * ({@code verification-base-url}) con el token plano como query param; el front luego llama a
 * {@code POST /register/verify}. Con {@code vetsoftware.mail.enabled=false} no envia (dev sin SMTP);
 * en ese caso el fallo se propaga solo si el envio esta habilitado y falla, para no dejar cuentas
 * imposibles de verificar (el registro completo hace rollback).
 */
@Component
public class JavaMailVerificationEmailSender implements VerificationEmailSender {

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean enabled;
    private final String verificationBaseUrl;

    public JavaMailVerificationEmailSender(
            JavaMailSender mailSender,
            @Value("${vetsoftware.mail.from}") String from,
            @Value("${vetsoftware.mail.enabled:true}") boolean enabled,
            @Value("${vetsoftware.registration.verification-base-url}") String verificationBaseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = enabled;
        this.verificationBaseUrl = verificationBaseUrl;
    }

    @Override
    public void send(String toEmail, String employeeName, String rawToken) {
        if (!enabled) return;
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalStateException("Cannot send verification email: missing recipient");
        }
        String link = buildLink(rawToken);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("Verifica tu cuenta de VetSoftware");
            helper.setText(buildHtml(employeeName, link), true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    private String buildLink(String rawToken) {
        String separator = verificationBaseUrl.contains("?") ? "&" : "?";
        return verificationBaseUrl + separator + "token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private String buildHtml(String employeeName, String link) {
        String name = (employeeName == null || employeeName.isBlank()) ? "" : " " + employeeName;
        return "<div style=\"font-family:Arial,sans-serif;font-size:14px;color:#222\">"
                + "<p>Hola" + escape(name) + ",</p>"
                + "<p>Gracias por registrarte en VetSoftware. Para activar tu cuenta y poder iniciar sesion, "
                + "confirma tu correo haciendo clic en el siguiente boton:</p>"
                + "<p><a href=\"" + link + "\" "
                + "style=\"display:inline-block;padding:10px 18px;background:#2563eb;color:#fff;"
                + "text-decoration:none;border-radius:6px\">Verificar mi cuenta</a></p>"
                + "<p>Si el boton no funciona, copia y pega este enlace en tu navegador:<br>"
                + "<a href=\"" + link + "\">" + link + "</a></p>"
                + "<p>Este enlace vence en unas horas. Si no creaste esta cuenta, ignora este mensaje.</p>"
                + "</div>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
