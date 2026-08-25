package com.vetsoftware.app.employee.infrastructure.email;

import com.vetsoftware.app.employee.application.port.out.EmployeeInvitationEmailSender;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Correo de invitación de empleado enviado con una <b>plantilla de Resend</b>
 * ({@code template: {
 * id, variables }}). Async y no bloqueante (si Resend falla, se registra un
 * warning y el alta continúa).
 *
 * <p>
 * Variables de la plantilla (coinciden con los {@code {{{VARIABLE}}}} del HTML
 * en Resend): EMPLOYEE_NAME, COMPANY_NAME, EMPLOYEE_CODE, TEMP_PASSWORD,
 * ROLE_NAME, LOGIN_URL, EMPLOYEE_EMAIL.
 */
@Component
public class ResendEmployeeInvitationEmailSender implements EmployeeInvitationEmailSender {

    private static final Logger log = LoggerFactory
            .getLogger(ResendEmployeeInvitationEmailSender.class);

    private static final String SUBJECT = "Tu cuenta de Vetrina está lista";

    private final ResendEmailClient email;
    private final String templateId;
    private final String loginUrl;

    // El default vacio de la configuracion NO es la politica: es lo que permite que
    // el contrato OpenAPI, las rodajas de test y el perfil local arranquen sin
    // declarar nada. Quien decide si un valor ausente es tolerable es
    // requireConfiguredWhenEmailIsEnabled(), abajo.
    public ResendEmployeeInvitationEmailSender(ResendEmailClient email,
            @Value("${vetsoftware.employee.invitation-template-id:}") String templateId,
            @Value("${vetsoftware.employee.login-url:}") String loginUrl) {
        this.email = email;
        this.templateId = templateId;
        this.loginUrl = loginUrl;
        requireConfiguredWhenEmailIsEnabled();
    }

    @Override
    public void send(String toEmail, String employeeName, String companyName, String employeeCode,
            String tempPassword, String roleName) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("EMPLOYEE_NAME", nz(employeeName));
        variables.put("COMPANY_NAME", nz(companyName));
        variables.put("EMPLOYEE_CODE", nz(employeeCode));
        variables.put("TEMP_PASSWORD", nz(tempPassword));
        variables.put("ROLE_NAME", nz(roleName));
        variables.put("LOGIN_URL", nz(loginUrl));
        variables.put("EMPLOYEE_EMAIL", nz(toEmail));

        email.sendTemplate(toEmail, null, SUBJECT, templateId, variables);
    }

    /**
     * Fallo al arrancar, y solo cuando el correo esta habilitado.
     *
     * <p>
     * Con {@code invitation-template-id} vacio, {@code sendTemplate} escribe un
     * warning y retorna: la app levanta, el alta responde con exito, y la
     * invitacion nunca sale. El empleado queda creado sin conocer su codigo de
     * usuario ni su contrasena temporal, que solo viajan por ese correo, y el admin
     * que lo dio de alta cree que ya puede entrar. {@code login-url} entra en la
     * misma cuenta: sin ella el correo sale sin destino al que ir. Mientras el
     * identificador viajo commiteado como default, esa red existia por accidente;
     * con el valor fuera de la imagen, la unica red es esta.
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
        requireConfigured(templateId, "vetsoftware.employee.invitation-template-id");
        requireConfigured(loginUrl, "vetsoftware.employee.login-url");
    }

    private static void requireConfigured(String value, String key) {
        if (value == null || value.isBlank()) {
            log.error("{} sin valor con el correo habilitado; la aplicacion no arrancara: la"
                    + " invitacion no saldria y el empleado quedaria creado sin conocer su codigo"
                    + " ni su contrasena temporal, sin que nadie se entere", key);
            throw new IllegalStateException(
                    "Configuracion del correo de invitacion de empleado incompleta: " + key);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
