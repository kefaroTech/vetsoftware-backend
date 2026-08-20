package com.vetsoftware.app.coderecovery.infrastructure.email;

import com.vetsoftware.app.coderecovery.application.port.out.CodeRecoveryEmailSender;
import com.vetsoftware.app.coderecovery.application.port.out.EmployeeAccountsByEmailPort.EmployeeAccount;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import com.vetsoftware.app.infrastructure.logging.DevEmailPreview;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Correo "recordar código" con la lista de cuentas del usuario. A diferencia de
 * los demás correos, este NO usa una plantilla de Resend: como el nº de cuentas
 * es variable y Resend limita cada variable a 2.000 caracteres, el listado se
 * rompería. En su lugar renderizamos el HTML completo aquí (plantilla en
 * {@code resources/email-templates/recover-code.html}) y lo enviamos como
 * cuerpo HTML — sin ese límite.
 *
 * <p>
 * La plantilla se carga <b>al construir el bean</b>: si el recurso no está en
 * el classpath (empaquetado roto) el contexto de Spring no arranca. Un
 * despliegue que no levanta es preferible a uno que acepta peticiones y
 * descarta el 100 % de los correos en silencio — el endpoint responde 204
 * siempre por anti-enumeración, así que el usuario nunca vería el fallo.
 *
 * <p>
 * Una vez construido, el envío es async/best-effort (nunca lanza; si Resend
 * falla, se registra un warning y el flujo continúa).
 */
@Component
public class ResendCodeRecoveryEmailSender implements CodeRecoveryEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendCodeRecoveryEmailSender.class);
    private static final String SUBJECT = "Tu código de usuario de Vetrina";
    private static final String TEMPLATE_PATH = "email-templates/recover-code.html";

    // Fila del listado (usa las clases .acct-* del <style> de la plantilla). {DIV}
    // = divisor salvo en
    // la primera.
    private static final String ROW_TEMPLATE = "<tr><td class=\"acct-cell stack{DIV}\" valign=\"middle\">"
            + "<div class=\"acct-lbl\">Veterinaria</div><div class=\"acct-co\">{COMPANY}</div></td>"
            + "<td class=\"acct-cell stack{DIV}\" align=\"right\" valign=\"middle\">"
            + "<span class=\"acct-code\">{CODE}</span></td></tr>";

    private final ResendEmailClient email;
    private final String loginUrl;
    private final String template;

    public ResendCodeRecoveryEmailSender(ResendEmailClient email,
            @Value("${vetsoftware.code-recovery.login-url:}") String loginUrl) {
        this.email = email;
        this.loginUrl = loginUrl;
        this.template = loadTemplate();
    }

    @Override
    public void send(String toEmail, String employeeName, List<EmployeeAccount> accounts) {
        if (!email.isEnabled()) {
            String preview = accounts.stream().map(a -> a.companyName() + "=" + a.code())
                    .collect(Collectors.joining(", "));
            DevEmailPreview.show(toEmail, "Códigos de usuario", preview);
            return;
        }
        String html = template.replace("{{{EMPLOYEE_NAME}}}", htmlEscape(nz(employeeName)))
                .replace("{{{ACCOUNTS_HTML}}}", buildAccountsHtml(accounts))
                .replace("{{{LOGIN_URL}}}", htmlEscape(nz(loginUrl)))
                .replace("{{{EMPLOYEE_EMAIL}}}", htmlEscape(nz(toEmail)));

        email.send(toEmail, null, SUBJECT, html, null);
    }

    /**
     * Lee la plantilla del classpath una sola vez, al construir el bean. Si el
     * recurso falta o no se puede leer, se registra a nivel ERROR con la excepción
     * (para distinguir "ausente del jar" de "fallo de lectura") y se propaga: el
     * arranque falla en vez de descartar cada correo en silencio.
     */
    private static String loadTemplate() {
        try {
            return new ClassPathResource(TEMPLATE_PATH).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("No se pudo cargar la plantilla de correo {}; la aplicación no arrancará",
                    TEMPLATE_PATH, e);
            throw new IllegalStateException(
                    "Plantilla de correo ausente o ilegible en el classpath: " + TEMPLATE_PATH, e);
        }
    }

    private String buildAccountsHtml(List<EmployeeAccount> accounts) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (EmployeeAccount a : accounts) {
            sb.append(ROW_TEMPLATE.replace("{DIV}", first ? "" : " acct-div")
                    .replace("{COMPANY}", htmlEscape(a.companyName()))
                    .replace("{CODE}", htmlEscape(a.code())));
            first = false;
        }
        return sb.toString();
    }

    /**
     * Escapa el texto que se inyecta como HTML (nombres de
     * veterinaria/códigos/nombre) para no romper el markup.
     */
    private static String htmlEscape(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
