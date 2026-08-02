package com.vetsoftware.app.passwordreset.infrastructure.email;

import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import com.vetsoftware.app.infrastructure.logging.DevEmailPreview;
import com.vetsoftware.app.passwordreset.application.port.out.PasswordResetEmailSender;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Correo de restablecimiento de contraseña enviado con una <b>plantilla de
 * Resend</b> ({@code
 * template: { id, variables }}). El enlace apunta a la página del front
 * ({@code reset-base-url}) con el token plano; el front llama a
 * {@code POST /auth/reset-password}. Async/best-effort: si Resend falla o el
 * envío está deshabilitado (dev), se registra el enlace/aviso en el log y el
 * flujo continúa.
 *
 * <p>
 * Variables de la plantilla (coinciden con los {@code {{{VARIABLE}}}} del HTML
 * en Resend): EMPLOYEE_NAME, COMPANY_NAME, EMPLOYEE_CODE, RESET_URL,
 * EMPLOYEE_EMAIL.
 */
@Component
public class ResendPasswordResetEmailSender implements PasswordResetEmailSender {

    private static final String SUBJECT = "Restablece tu contraseña de Vetrina";

    private final ResendEmailClient email;
    private final String resetBaseUrl;
    private final String templateId;

    public ResendPasswordResetEmailSender(ResendEmailClient email,
            @Value("${vetsoftware.password-reset.reset-base-url}") String resetBaseUrl,
            @Value("${vetsoftware.password-reset.template-id:}") String templateId) {
        this.email = email;
        this.resetBaseUrl = resetBaseUrl;
        this.templateId = templateId;
    }

    @Override
    public void send(String toEmail, String employeeName, String employeeCode, String companyName,
            String rawToken) {
        String link = buildLink(rawToken);
        if (!email.isEnabled()) {
            // El enlace lleva el token de restablecimiento en claro: va por el canal de
            // previsualización local, que no alcanza el pipeline exportado (ver
            // DevEmailPreview).
            DevEmailPreview.show(toEmail, "Enlace de restablecimiento", link);
            return;
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("EMPLOYEE_NAME", nz(employeeName));
        variables.put("COMPANY_NAME", nz(companyName));
        variables.put("EMPLOYEE_CODE", nz(employeeCode));
        variables.put("RESET_URL", link);
        variables.put("EMPLOYEE_EMAIL", nz(toEmail));

        email.sendTemplate(toEmail, null, SUBJECT, templateId, variables);
    }

    private String buildLink(String rawToken) {
        String separator = resetBaseUrl.contains("?") ? "&" : "?";
        return resetBaseUrl + separator + "token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
