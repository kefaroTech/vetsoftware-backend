package com.vetsoftware.app.employee.infrastructure.email;

import com.vetsoftware.app.employee.application.port.out.EmployeeInvitationEmailSender;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import java.util.LinkedHashMap;
import java.util.Map;
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

    private static final String SUBJECT = "Tu cuenta de Vetrina está lista";

    private final ResendEmailClient email;
    private final String templateId;
    private final String loginUrl;

    public ResendEmployeeInvitationEmailSender(ResendEmailClient email,
            @Value("${vetsoftware.employee.invitation-template-id:}") String templateId,
            @Value("${vetsoftware.employee.login-url:}") String loginUrl) {
        this.email = email;
        this.templateId = templateId;
        this.loginUrl = loginUrl;
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

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
