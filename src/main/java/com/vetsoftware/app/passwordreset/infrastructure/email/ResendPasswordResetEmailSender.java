package com.vetsoftware.app.passwordreset.infrastructure.email;

import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import com.vetsoftware.app.infrastructure.logging.DevEmailPreview;
import com.vetsoftware.app.passwordreset.application.port.out.PasswordResetEmailSender;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ResendPasswordResetEmailSender.class);

    private static final String SUBJECT = "Restablece tu contraseña de Vetrina";

    private final ResendEmailClient email;
    private final String resetBaseUrl;
    private final String templateId;

    // El default vacio de la configuracion NO es la politica: es lo que permite que
    // el contrato OpenAPI, las rodajas de test y el perfil local arranquen sin
    // declarar nada. Quien decide si un valor ausente es tolerable es
    // requireConfiguredWhenEmailIsEnabled(), abajo.
    public ResendPasswordResetEmailSender(ResendEmailClient email,
            @Value("${vetsoftware.password-reset.reset-base-url}") String resetBaseUrl,
            @Value("${vetsoftware.password-reset.template-id:}") String templateId) {
        this.email = email;
        this.resetBaseUrl = resetBaseUrl;
        this.templateId = templateId;
        requireConfiguredWhenEmailIsEnabled();
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

    /**
     * Fallo al arrancar, y solo cuando el correo esta habilitado.
     *
     * <p>
     * Con {@code template-id} vacio, {@code sendTemplate} escribe un warning y
     * retorna: la app levanta, la peticion de "olvide mi contrasena" responde con
     * exito, y el enlace de restablecimiento nunca sale. Quien perdio su contrasena
     * se queda sin ninguna via de recuperarla, y no puede saberlo: la respuesta es
     * identica por diseno anti-enumeracion. Mientras el identificador viajo
     * commiteado como default, esa red existia por accidente; con el valor fuera de
     * la imagen, la unica red es esta.
     *
     * <p>
     * El default vacio existe para que el contrato OpenAPI, las rodajas de test y
     * el perfil local, que declaran {@code vetsoftware.email.enabled=false},
     * arranquen sin declarar nada; ninguno de ellos pasa por aqui. Los unicos que
     * si lo hacen son dev y prod, que declaran {@code enabled: true}, que es
     * exactamente donde el silencio cuesta.
     */
    private void requireConfiguredWhenEmailIsEnabled() {
        if (!email.isEnabled()) {
            return;
        }
        requireConfigured(resetBaseUrl, "vetsoftware.password-reset.reset-base-url");
        requireConfigured(templateId, "vetsoftware.password-reset.template-id");
    }

    private static void requireConfigured(String value, String key) {
        if (value == null || value.isBlank()) {
            log.error("{} sin valor con el correo habilitado; la aplicacion no arrancara: el enlace"
                    + " de restablecimiento no saldria y quien perdio su contrasena se quedaria sin"
                    + " via de recuperarla, sin que nadie se entere", key);
            throw new IllegalStateException(
                    "Configuracion del correo de restablecimiento incompleta: " + key);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
