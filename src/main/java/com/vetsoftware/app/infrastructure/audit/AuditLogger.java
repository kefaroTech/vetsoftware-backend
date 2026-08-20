package com.vetsoftware.app.infrastructure.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Emisor central de eventos de auditoría (OWASP ASVS V7.1.2 / 7.1.4).
 *
 * <p>
 * Escribe al logger {@code "AUDIT"}, que sale por el pipeline estructurado
 * (stdout JSON → CloudWatch → Loki) para consulta en Grafana. Los campos del
 * actor ({@code
 * actor.type} / {@code actor.companyId} / {@code actor.employeeId} /
 * {@code actor.systemUserId}, poblados en {@code AuthFilter}) y el contexto
 * HTTP de la request ({@code client.ip} / {@code
 * user_agent.original} / {@code http.method} / {@code http.path}, poblados en
 * {@code
 * RequestLoggingContextFilter} para toda request — ver
 * {@link com.vetsoftware.app.infrastructure.logging.MdcKeys}) viajan por el
 * MDC, y el encoder estructurado de Spring Boot los emite como campos JSON;
 * aquí solo se añaden los campos propios del evento (no se duplican
 * {@code http.method}/{@code http.path} ya presentes en el MDC).
 *
 * <p>
 * <b>El log es hoy el único destino.</b> El outbox que persistía el mismo hecho
 * y lo archivaba en S3 Object Lock vía Firehose se retiró deliberadamente
 * (junto con su cadena de integridad). La consecuencia hay que tenerla presente
 * y no descubrirla en una auditoría: este rastro es <b>mutable y de retención
 * acotada</b> —la que dé Loki—, así que no constituye evidencia inalterable. Si
 * aparece una obligación de conservación o de no repudio (ASVS V7, NIST SP
 * 800-53 AU-9, ISO/IEC 27001 A.8.15), hay que reponer un destino durable; el
 * historial de git conserva la implementación anterior.
 *
 * <p>
 * Convención de campos: notación con punto ({@code http.*}, {@code actor.*})
 * alineada con OpenTelemetry. Nota: {@code actor.identifier} (en login) es el
 * <em>código</em> de empleado/usuario tal como llega en la request — distinto
 * de {@code actor.employeeId} del MDC, que es el id numérico del principal ya
 * autenticado.
 *
 * <p>
 * Nunca registra credenciales ni tokens — solo identificadores no sensibles.
 */
@Component
public class AuditLogger {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    /**
     * Mutación de recurso en el borde HTTP (POST/PUT/PATCH/DELETE).
     * http.method/http.path vía MDC.
     */
    public void mutation(String method, String path, int status, String outcome, long durationMs) {
        audit.atInfo().addKeyValue("event", "http_mutation").addKeyValue("http.status", status)
                .addKeyValue("outcome", outcome).addKeyValue("http.durationMs", durationMs)
                .log("mutation {} {} -> {} ({})", method, path, status, outcome);
    }

    /**
     * Alta de una nueva empresa (veterinaria) por auto-registro público. Sin datos
     * sensibles; {@code
     * ownerCode} es el código de acceso del dueño, no un secreto.
     * client.ip/http.path vía MDC.
     */
    public void companyRegistered(Long companyId, String companyName, String companyIdentifier,
            Long ownerEmployeeId, String ownerCode) {
        audit.atInfo().addKeyValue("event", "company_registered")
                .addKeyValue("company.id", companyId).addKeyValue("company.name", companyName)
                .addKeyValue("company.identifier", companyIdentifier)
                .addKeyValue("actor.employeeId", ownerEmployeeId)
                .addKeyValue("actor.identifier", ownerCode).addKeyValue("outcome", "SUCCESS")
                .log("company registered id={} name={}", companyId, companyName);
    }

    /**
     * Un admin (o quien tenga el permiso) envió una invitación de acceso a un nuevo
     * empleado. El actor que invita viaja en el MDC (actor.employeeId /
     * actor.companyId). {@code invitedCode} es el código de acceso del invitado, no
     * un secreto.
     */
    public void employeeInvited(Long invitedEmployeeId, String invitedCode, Long companyId) {
        audit.atInfo().addKeyValue("event", "employee_invited")
                .addKeyValue("employee.id", invitedEmployeeId)
                .addKeyValue("employee.identifier", invitedCode)
                .addKeyValue("company.id", companyId).addKeyValue("outcome", "SUCCESS")
                .log("employee invited id={} code={}", invitedEmployeeId, invitedCode);
    }

    /**
     * Reenvío de la invitación (nueva contraseña provisional) por un admin/con
     * permiso. Actor vía MDC.
     */
    public void employeeInvitationResent(Long invitedEmployeeId, String invitedCode,
            Long companyId) {
        audit.atInfo().addKeyValue("event", "employee_invitation_resent")
                .addKeyValue("employee.id", invitedEmployeeId)
                .addKeyValue("employee.identifier", invitedCode)
                .addKeyValue("company.id", companyId).addKeyValue("outcome", "SUCCESS")
                .log("employee invitation resent id={} code={}", invitedEmployeeId, invitedCode);
    }

    /**
     * El empleado invitado aceptó la invitación: completó su primer ingreso
     * cambiando la contraseña temporal (INVITED → activo con contraseña propia). El
     * propio empleado es el actor (MDC).
     */
    public void invitationAccepted(Long employeeId, Long companyId) {
        audit.atInfo().addKeyValue("event", "invitation_accepted")
                .addKeyValue("employee.id", employeeId).addKeyValue("company.id", companyId)
                .addKeyValue("outcome", "SUCCESS")
                .log("invitation accepted employeeId={}", employeeId);
    }

    /**
     * Login exitoso; {@code identifier} es el código de empleado/usuario, no un
     * secreto.
     */
    public void loginSuccess(String userType, String identifier) {
        audit.atInfo().addKeyValue("event", "login_success").addKeyValue("actor.type", userType)
                .addKeyValue("actor.identifier", identifier).addKeyValue("outcome", "SUCCESS")
                .log("login success type={} id={}", userType, identifier);
    }

    /**
     * Intento de login fallido; sin identificador (no disponible en el handler) ni
     * credenciales. http.path vía MDC.
     */
    public void loginFailure(String path, String reason) {
        audit.atWarn().addKeyValue("event", "login_failure").addKeyValue("outcome", "FAILURE")
                .addKeyValue("reason", reason).log("login failure {} reason={}", path, reason);
    }

    /**
     * Login bloqueado porque el correo del empleado aún no está verificado
     * (auto-registro Opción B). {@code identifier} es el código de empleado
     * intentado (no un secreto). http.path/client.ip vía MDC.
     */
    public void loginBlockedEmailNotVerified(String identifier) {
        audit.atWarn().addKeyValue("event", "login_blocked_email_not_verified")
                .addKeyValue("actor.identifier", identifier).addKeyValue("outcome", "DENIED")
                .addKeyValue("reason", "email_not_verified")
                .log("login blocked, email not verified id={}", identifier);
    }

    /**
     * Se presentó un refresh token ya revocado y se revocó la familia entera del
     * sujeto. Es la señal canónica de robo del OAuth 2.0 Security BCP (§4.14.2), y
     * el único evento de este archivo que describe un ataque en curso y no una
     * decisión de autorización: merece revisión humana, no solo quedar registrado.
     *
     * <p>
     * {@code seconds_since_revocation} es lo que permite separar el robo de una
     * carrera benigna entre pestañas al leer el log. Valores de pocos segundos por
     * encima de la ventana de gracia apuntan a lo segundo; horas o días, a lo
     * primero.
     */
    public void refreshTokenReuseDetected(Long subjectId, String subjectType,
            long secondsSinceRevocation) {
        audit.atWarn().addKeyValue("event", "refresh_token_reuse_detected")
                .addKeyValue("actor.id", subjectId).addKeyValue("actor.type", subjectType)
                .addKeyValue("outcome", "DENIED")
                .addKeyValue("seconds_since_revocation", secondsSinceRevocation)
                .log("refresh token reuse detected type={} id={}; revoked all sessions",
                        subjectType, subjectId);
    }

    /**
     * Denegación de autorización (@PreAuthorize → AccessDeniedException). Actor,
     * http.* vía MDC.
     */
    public void accessDenied(String method, String path) {
        audit.atWarn().addKeyValue("event", "access_denied").addKeyValue("outcome", "DENIED")
                .log("access denied {} {}", method, path);
    }

    /**
     * Acceso a un recurso protegido sin autenticación válida (token
     * ausente/inválido → 401). http.method/http.path vía MDC.
     *
     * <p>
     * <b>INFO y no WARN, deliberadamente.</b> El criterio del repo para la
     * severidad es quién debe actuar, no cuán grave suena el hecho: un 401 por
     * token caducado es el desenlace más rutinario que tiene una API con sesiones
     * —el front lo trata refrescando el token— y cualquier anónimo puede provocarlo
     * a voluntad. El sistema funcionó exactamente como debía, así que no hay nada
     * que un operador tenga que hacer. A nivel WARN este evento es la población
     * dominante del canal AUDIT y entierra los WARN que sí piden revisión humana
     * ({@code refresh_token_reuse_detected},
     * {@code login_blocked_email_not_verified}, {@code rate_limited}). El hecho
     * sigue registrado y consultable; lo que cambia es que deja de pedir atención.
     *
     * <p>
     * Un pico de 401 no se vigila leyendo este log: se vigila con la tasa de 401 y
     * con el contador {@code vetsoftware.security.tokens.rejected} de
     * {@code AuthFilter}, que separa el fallo aislado del fallo sistémico. La
     * severidad no es el mecanismo de alerta.
     *
     * <p>
     * {@code reason} es <b>vocabulario cerrado en snake_case</b>, alineado con el
     * {@code code} del ProblemDetail que ve el front: {@code token_missing},
     * {@code token_expired}, {@code token_invalid}, {@code session_replaced}. Nunca
     * un mensaje de excepción ni texto libre: {@code reason} se agrupa y se filtra
     * en Grafana, y un valor no acotado lo vuelve inagrupable.
     */
    public void unauthenticated(String method, String path, String reason) {
        audit.atInfo().addKeyValue("event", "unauthenticated").addKeyValue("outcome", "DENIED")
                .addKeyValue("reason", reason)
                .log("unauthenticated {} {} reason={}", method, path, reason);
    }

    /**
     * Login bloqueado por rate limiting (429) — señal de fuerza bruta.
     * client.ip/http.path vía MDC.
     */
    public void loginRateLimited() {
        rateLimited("LOGIN_RATE_LIMITED");
    }

    /**
     * Ruta publica bloqueada por rate limiting (429). client.ip/http.path via MDC.
     */
    public void rateLimited(String code) {
        audit.atWarn().addKeyValue("event", "rate_limited").addKeyValue("code", code)
                .addKeyValue("outcome", "DENIED").log("rate limited code={}", code);
    }
}
