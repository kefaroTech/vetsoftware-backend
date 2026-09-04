package com.vetsoftware.app.platformaccess.infrastructure.email;

import com.vetsoftware.app.infrastructure.email.EmailDispatchOutcome;
import com.vetsoftware.app.infrastructure.email.HtmlEscaper;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import com.vetsoftware.app.infrastructure.logging.DevEmailPreview;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessEmailSender;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics.InvitationResult;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Los cuatro correos del alta de superadministradores, con plantillas
 * server-side de Resend.
 *
 * <p>
 * <b>El asunto es una constante en los cuatro, y eso no es estilo.</b> Un
 * asunto construido con el nombre de quien solicita permitiría inyectar
 * cabeceras con un salto de línea. Por la misma familia de razones,
 * {@code FULL_NAME} y {@code REASON} pasan por {@link HtmlEscaper} antes de
 * entrar en el mapa: las plantillas de este repositorio usan triple llave, que
 * es inserción sin escapar, y aquí el texto lo escribe un anónimo mientras el
 * destinatario es quien puede crear superadministradores. La otra mitad de esa
 * defensa es usar doble llave para esas dos variables al crear la plantilla en
 * Resend.
 *
 * <p>
 * <b>De los cuatro, solo la invitación vigila su desenlace</b>, y es una
 * decisión con motivo: es el único cuya pérdida deja una cuenta aprobada que
 * nunca llega a existir, sin que nadie se entere. Leerlo obliga a componer
 * sobre el futuro que devuelve el envío; un {@code try/catch} alrededor de la
 * llamada sería código muerto para el 100 % de los fallos reales, porque el
 * envío es {@code @Async} y por contrato no lanza.
 *
 * <p>
 * <b>El envío deshabilitado no es un fallo.</b> Es el modo normal de desarrollo
 * y se cuenta como {@code skipped}: tratarlo como pérdida llenaría de falsos
 * positivos cualquier alerta de tasa. En ese modo el token y el código en claro
 * salen por {@link DevEmailPreview}, que es el único canal sin redacción del
 * sistema, existe solo en el perfil local y no tiene ruta hasta el pipeline
 * exportado. Es el sitio correcto para ellos, y el único.
 */
@Component
public class ResendPlatformAccessEmailSender implements PlatformAccessEmailSender {

    private static final Logger log = LoggerFactory
            .getLogger(ResendPlatformAccessEmailSender.class);

    private static final String SUBJECT_REQUEST = "Nueva solicitud de acceso de plataforma";
    private static final String SUBJECT_APPROVED = "Tu acceso de plataforma fue aprobado";
    private static final String SUBJECT_REJECTED = "Sobre tu solicitud de acceso de plataforma";
    private static final String SUBJECT_WELCOME = "Tu cuenta de plataforma de Lumbre";

    private static final DateTimeFormatter REQUESTED_AT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm");

    private final ResendEmailClient email;
    private final PlatformAccessAuditPort audit;
    private final PlatformAccessMetrics metrics;
    private final String approverEmail;
    private final String reviewBaseUrl;
    private final String invitationBaseUrl;
    private final String loginUrl;
    private final String requestTemplateId;
    private final String approvedTemplateId;
    private final String rejectedTemplateId;
    private final String welcomeTemplateId;
    private final String helpUrl;
    private final String privacyUrl;
    private final String termsUrl;

    // Todos los @Value llevan default vacío a propósito, y el default NO es la
    // política: es lo que permite que el contexto del contrato OpenAPI y las
    // rodajas de test —que no cargan dev ni prod— arranquen sin declarar nada.
    // Quien decide si un valor ausente es tolerable es
    // requireConfiguredWhenEmailIsEnabled(), abajo.
    @SuppressWarnings("java:S107")
    public ResendPlatformAccessEmailSender(ResendEmailClient email, PlatformAccessAuditPort audit,
            PlatformAccessMetrics metrics,
            @Value("${vetsoftware.platform-access.approver-email:}") String approverEmail,
            @Value("${vetsoftware.platform-access.review-base-url:}") String reviewBaseUrl,
            @Value("${vetsoftware.platform-access.invitation-base-url:}") String invitationBaseUrl,
            @Value("${vetsoftware.platform-access.login-url:}") String loginUrl,
            @Value("${vetsoftware.platform-access.request-template-id:}") String requestTemplateId,
            @Value("${vetsoftware.platform-access.approved-template-id:}") String approvedTemplateId,
            @Value("${vetsoftware.platform-access.rejected-template-id:}") String rejectedTemplateId,
            @Value("${vetsoftware.platform-access.welcome-template-id:}") String welcomeTemplateId,
            @Value("${vetsoftware.email.help-url:}") String helpUrl,
            @Value("${vetsoftware.email.privacy-url:}") String privacyUrl,
            @Value("${vetsoftware.email.terms-url:}") String termsUrl) {
        this.email = email;
        this.audit = audit;
        this.metrics = metrics;
        this.approverEmail = approverEmail;
        this.reviewBaseUrl = reviewBaseUrl;
        this.invitationBaseUrl = invitationBaseUrl;
        this.loginUrl = loginUrl;
        this.requestTemplateId = requestTemplateId;
        this.approvedTemplateId = approvedTemplateId;
        this.rejectedTemplateId = rejectedTemplateId;
        this.welcomeTemplateId = welcomeTemplateId;
        this.helpUrl = helpUrl;
        this.privacyUrl = privacyUrl;
        this.termsUrl = termsUrl;
        requireConfiguredWhenEmailIsEnabled();
    }

    @Override
    public void sendAccessRequested(AccessRequestedNotification notification) {
        String link = buildLink(reviewBaseUrl, notification.rawApprovalToken());
        if (!email.isEnabled()) {
            DevEmailPreview.show(approverEmail, "Enlace de aprobacion de plataforma",
                    link + " codigo=" + notification.verificationCode());
            return;
        }
        Map<String, Object> variables = footer();
        variables.put("FULL_NAME", HtmlEscaper.escape(notification.fullName()));
        variables.put("REQUESTER_EMAIL", nz(notification.requesterEmail()));
        variables.put("REASON", HtmlEscaper.escape(notification.reason()));
        variables.put("REQUESTED_AT", format(notification.requestedAt()));
        variables.put("REVIEW_URL", link);
        variables.put("VERIFICATION_CODE", nz(notification.verificationCode()));

        email.sendTemplate(approverEmail, null, SUBJECT_REQUEST, requestTemplateId, variables);
    }

    @Override
    public void sendInvitation(Long requestId, String toEmail, String fullName,
            String rawInvitationToken) {
        String link = buildLink(invitationBaseUrl, rawInvitationToken);
        if (!email.isEnabled()) {
            DevEmailPreview.show(toEmail, "Enlace de invitacion de plataforma", link);
            metrics.invitation(InvitationResult.SKIPPED);
            return;
        }
        Map<String, Object> variables = footer();
        variables.put("FULL_NAME", HtmlEscaper.escape(fullName));
        variables.put("INVITATION_URL", link);

        String domain = domainOf(toEmail);
        email.sendTemplate(toEmail, null, SUBJECT_APPROVED, approvedTemplateId, variables)
                .thenAccept(outcome -> recordInvitationOutcome(requestId, domain, outcome));
    }

    @Override
    public void sendRejection(Long requestId, String toEmail, String fullName) {
        if (!email.isEnabled()) {
            DevEmailPreview.show(toEmail, "Aviso de rechazo de acceso de plataforma",
                    "(sin datos)");
            return;
        }
        Map<String, Object> variables = footer();
        variables.put("FULL_NAME", HtmlEscaper.escape(fullName));

        email.sendTemplate(toEmail, null, SUBJECT_REJECTED, rejectedTemplateId, variables);
    }

    @Override
    public void sendWelcome(Long requestId, String toEmail, String fullName,
            String systemUserCode) {
        if (!email.isEnabled()) {
            DevEmailPreview.show(toEmail, "Codigo de usuario de plataforma", systemUserCode);
            return;
        }
        Map<String, Object> variables = footer();
        variables.put("FULL_NAME", HtmlEscaper.escape(fullName));
        // El codigo de usuario NO es un secreto —es el equivalente al nombre de
        // usuario— y sin el la cuenta recien creada no se puede usar: el login de
        // las cuentas de sistema es por codigo, no por correo.
        variables.put("SYSTEM_USER_CODE", nz(systemUserCode));
        variables.put("LOGIN_URL", nz(loginUrl));

        String domain = domainOf(toEmail);
        email.sendTemplate(toEmail, null, SUBJECT_WELCOME, welcomeTemplateId, variables)
                .thenAccept(outcome -> recordWelcomeOutcome(requestId, domain, outcome));
    }

    /**
     * Traduce el desenlace del envío de la invitación. Corre en el hilo del pool de
     * correo, ya después del commit, así que el id de la solicitud viaja como
     * argumento y no confiado al MDC.
     */
    private void recordInvitationOutcome(Long requestId, String emailDomain,
            EmailDispatchOutcome outcome) {
        switch (outcome) {
            case ACCEPTED -> {
                audit.invited(requestId, emailDomain);
                metrics.invitation(InvitationResult.SENT);
            }
            case SKIPPED -> metrics.invitation(InvitationResult.SKIPPED);
            case FAILED -> {
                audit.invitationUndelivered(requestId, emailDomain);
                metrics.invitation(InvitationResult.FAILED);
            }
        }
    }

    /**
     * Traduce el desenlace del envío de la bienvenida.
     *
     * <p>
     * <b>Se vigila por el mismo motivo que la invitación, no por simetría.</b> El
     * login de las cuentas de sistema es por {@code code}, no por correo, y este es
     * el único canal por el que el nuevo superadministrador conoce el suyo: si el
     * correo se pierde, la cuenta existe, tiene control total de la plataforma y su
     * dueño no puede entrar. Es tan terminal como la invitación perdida, así que
     * comparte su severidad.
     *
     * <p>
     * <b>No toca ningún contador.</b> {@code InvitationResult} cuenta la
     * invitación, y meter en esa serie los desenlaces de un correo distinto haría
     * que «invitaciones enviadas» dejara de significar lo que dice sin que nada lo
     * delate. El hecho terminal queda en el evento de auditoría, que es donde una
     * persona lo puede accionar.
     */
    private void recordWelcomeOutcome(Long requestId, String emailDomain,
            EmailDispatchOutcome outcome) {
        if (outcome == EmailDispatchOutcome.FAILED) {
            audit.welcomeUndelivered(requestId, emailDomain);
        }
    }

    /**
     * Los tres enlaces del pie ya existen para todo el correo del sistema; no se
     * duplican por feature.
     */
    private Map<String, Object> footer() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("HELP_URL", nz(helpUrl));
        variables.put("PRIVACY_URL", nz(privacyUrl));
        variables.put("TERMS_URL", nz(termsUrl));
        return variables;
    }

    private static String buildLink(String baseUrl, String rawToken) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private static String format(LocalDateTime value) {
        return value == null ? "" : REQUESTED_AT.format(value);
    }

    private static String domainOf(String address) {
        if (address == null) {
            return "";
        }
        int at = address.lastIndexOf("@");
        return at < 0 || at == address.length() - 1 ? "" : address.substring(at + 1);
    }

    /**
     * Fallo al arrancar, y solo cuando el correo está habilitado.
     *
     * <p>
     * El precedente del repositorio para configuración incompleta es tumbar el
     * contexto —{@code ResendCodeRecoveryEmailSender} lo hace con su plantilla— y
     * aquí aplica igual: con una de estas ocho claves vacía la aplicación arranca,
     * {@code POST /platform/access-request} responde 202, {@code sendTemplate}
     * escribe un {@code warn} y retorna, y <b>el aprobador no recibe nada</b>. La
     * solicitud muere sin rastro y el solicitante no tiene forma de saberlo, porque
     * el 202 es idéntico por diseño anti-enumeración. Un despliegue que no levanta
     * es preferible a uno que descarta el 100 % de los correos en silencio.
     *
     * <p>
     * <b>La guarda es {@code email.isEnabled()}, y con eso basta.</b> El contexto
     * del contrato OpenAPI declara {@code vetsoftware.email.enabled: false}
     * ({@code application-openapi.yml}), las rodajas de test no levantan este bean
     * y el perfil local trae {@code EMAIL_ENABLED:false} por defecto: ninguno pasa
     * por aquí. Los únicos que sí lo hacen son dev y prod, que declaran
     * {@code enabled: true} — que es exactamente donde el silencio cuesta.
     */
    private void requireConfiguredWhenEmailIsEnabled() {
        if (!email.isEnabled()) {
            return;
        }
        requireConfigured(approverEmail, "vetsoftware.platform-access.approver-email");
        requireConfigured(reviewBaseUrl, "vetsoftware.platform-access.review-base-url");
        requireConfigured(invitationBaseUrl, "vetsoftware.platform-access.invitation-base-url");
        requireConfigured(loginUrl, "vetsoftware.platform-access.login-url");
        requireConfigured(requestTemplateId, "vetsoftware.platform-access.request-template-id");
        requireConfigured(approvedTemplateId, "vetsoftware.platform-access.approved-template-id");
        requireConfigured(rejectedTemplateId, "vetsoftware.platform-access.rejected-template-id");
        requireConfigured(welcomeTemplateId, "vetsoftware.platform-access.welcome-template-id");
    }

    private static void requireConfigured(String value, String key) {
        if (value == null || value.isBlank()) {
            log.error("{} sin valor con el correo habilitado; la aplicacion no arrancara: el correo"
                    + " correspondiente del alta de superadministradores no saldria y la solicitud"
                    + " moriria sin que nadie se entere", key);
            throw new IllegalStateException(
                    "Configuracion de correo del alta de superadministradores incompleta: " + key);
        }
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
