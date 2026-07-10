package com.vetsoftware.app.infrastructure.audit;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Emisor central de eventos de auditoría (OWASP ASVS V7.1.2 / 7.1.4).
 *
 * <p>Escribe al logger {@code "AUDIT"}, filtrable en Loki por {@code logger_name="AUDIT"}, y con
 * appender dedicado de retención larga ({@code logs/audit.log}, ver {@code logback-spring.xml}).
 * Los campos del actor ({@code actor.type} / {@code actor.companyId} / {@code actor.employeeId} /
 * {@code actor.systemUserId}, poblados en {@code AuthFilter}) y el contexto HTTP de la request
 * ({@code client.ip} / {@code user_agent.original} / {@code http.method} / {@code http.path}, poblados
 * en {@code TraceContextResetFilter} para toda request — ver
 * {@link com.vetsoftware.app.infrastructure.logging.MdcKeys}) viajan por el MDC, y el
 * {@code LogstashEncoder} los emite automáticamente como campos JSON; aquí solo se añaden los campos
 * propios del evento (no se duplican {@code http.method}/{@code http.path} ya presentes en el MDC).
 *
 * <p>Convención de campos: notación con punto ({@code http.*}, {@code actor.*}) alineada con
 * OpenTelemetry. Nota: {@code actor.identifier} (en login) es el <em>código</em> de empleado/usuario
 * tal como llega en la request — distinto de {@code actor.employeeId} del MDC, que es el id numérico
 * del principal ya autenticado.
 *
 * <p>Nunca registra credenciales ni tokens — solo identificadores no sensibles.
 */
@Component
public class AuditLogger {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    /** Mutación de recurso en el borde HTTP (POST/PUT/PATCH/DELETE). http.method/http.path vía MDC. */
    public void mutation(String method, String path, int status, String outcome, long durationMs) {
        audit.info("mutation {} {} -> {} ({})", method, path, status, outcome,
                kv("event", "http_mutation"),
                kv("http.status", status),
                kv("outcome", outcome),
                kv("http.durationMs", durationMs));
    }

    /** Alta de una nueva empresa (veterinaria) por auto-registro público. Sin datos sensibles;
     *  {@code ownerCode} es el código de acceso del dueño, no un secreto. client.ip/http.path vía MDC. */
    public void companyRegistered(Long companyId, String companyName, String companyIdentifier,
                                  Long ownerEmployeeId, String ownerCode) {
        audit.info("company registered id={} name={}", companyId, companyName,
                kv("event", "company_registered"),
                kv("company.id", companyId),
                kv("company.name", companyName),
                kv("company.identifier", companyIdentifier),
                kv("actor.employeeId", ownerEmployeeId),
                kv("actor.identifier", ownerCode),
                kv("outcome", "SUCCESS"));
    }

    /** Un admin (o quien tenga el permiso) envió una invitación de acceso a un nuevo empleado. El actor que
     *  invita viaja en el MDC (actor.employeeId / actor.companyId). {@code invitedCode} es el código de
     *  acceso del invitado, no un secreto. */
    public void employeeInvited(Long invitedEmployeeId, String invitedCode, Long companyId) {
        audit.info("employee invited id={} code={}", invitedEmployeeId, invitedCode,
                kv("event", "employee_invited"),
                kv("employee.id", invitedEmployeeId),
                kv("employee.identifier", invitedCode),
                kv("company.id", companyId),
                kv("outcome", "SUCCESS"));
    }

    /** Reenvío de la invitación (nueva contraseña provisional) por un admin/con permiso. Actor vía MDC. */
    public void employeeInvitationResent(Long invitedEmployeeId, String invitedCode, Long companyId) {
        audit.info("employee invitation resent id={} code={}", invitedEmployeeId, invitedCode,
                kv("event", "employee_invitation_resent"),
                kv("employee.id", invitedEmployeeId),
                kv("employee.identifier", invitedCode),
                kv("company.id", companyId),
                kv("outcome", "SUCCESS"));
    }

    /** El empleado invitado aceptó la invitación: completó su primer ingreso cambiando la contraseña
     *  temporal (INVITED → activo con contraseña propia). El propio empleado es el actor (MDC). */
    public void invitationAccepted(Long employeeId, Long companyId) {
        audit.info("invitation accepted employeeId={}", employeeId,
                kv("event", "invitation_accepted"),
                kv("employee.id", employeeId),
                kv("company.id", companyId),
                kv("outcome", "SUCCESS"));
    }

    /** Login exitoso; {@code identifier} es el código de empleado/usuario, no un secreto. */
    public void loginSuccess(String userType, String identifier) {
        audit.info("login success type={} id={}", userType, identifier,
                kv("event", "login_success"),
                kv("actor.type", userType),
                kv("actor.identifier", identifier),
                kv("outcome", "SUCCESS"));
    }

    /** Intento de login fallido; sin identificador (no disponible en el handler) ni credenciales.
     *  http.path vía MDC. */
    public void loginFailure(String path, String reason) {
        audit.warn("login failure {} reason={}", path, reason,
                kv("event", "login_failure"),
                kv("outcome", "FAILURE"),
                kv("reason", reason));
    }

    /** Login bloqueado porque el correo del empleado aún no está verificado (auto-registro Opción B).
     *  {@code identifier} es el código de empleado intentado (no un secreto). http.path/client.ip vía MDC. */
    public void loginBlockedEmailNotVerified(String identifier) {
        audit.warn("login blocked, email not verified id={}", identifier,
                kv("event", "login_blocked_email_not_verified"),
                kv("actor.identifier", identifier),
                kv("outcome", "DENIED"),
                kv("reason", "email_not_verified"));
    }

    /** Denegación de autorización (@PreAuthorize → AccessDeniedException). Actor, http.* vía MDC. */
    public void accessDenied(String method, String path) {
        audit.warn("access denied {} {}", method, path,
                kv("event", "access_denied"),
                kv("outcome", "DENIED"));
    }

    /** Acceso a un recurso protegido sin autenticación válida (token ausente/inválido → 401).
     *  http.method/http.path vía MDC. */
    public void unauthenticated(String method, String path, String reason) {
        audit.warn("unauthenticated {} {} reason={}", method, path, reason,
                kv("event", "unauthenticated"),
                kv("outcome", "DENIED"),
                kv("reason", reason));
    }

    /** Login bloqueado por rate limiting (429) — señal de fuerza bruta. client.ip/http.path vía MDC. */
    public void loginRateLimited() {
        rateLimited("LOGIN_RATE_LIMITED");
    }

    /** Ruta publica bloqueada por rate limiting (429). client.ip/http.path via MDC. */
    public void rateLimited(String code) {
        audit.warn("rate limited code={}", code,
                kv("event", "rate_limited"),
                kv("code", code),
                kv("outcome", "DENIED"));
    }
}
