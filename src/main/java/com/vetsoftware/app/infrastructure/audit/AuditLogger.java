package com.vetsoftware.app.infrastructure.audit;

import com.vetsoftware.app.infrastructure.audit.outbox.AuditEventStore;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Emisor central de eventos de auditoría (OWASP ASVS V7.1.2 / 7.1.4).
 *
 * <p>Escribe al logger {@code "AUDIT"} para consulta en Grafana y, en producción, persiste
 * el mismo hecho en el outbox que termina en S3 Object Lock mediante Firehose.
 * Los campos del actor ({@code actor.type} / {@code actor.companyId} / {@code actor.employeeId} /
 * {@code actor.systemUserId}, poblados en {@code AuthFilter}) y el contexto HTTP de la request
 * ({@code client.ip} / {@code user_agent.original} / {@code http.method} / {@code http.path}, poblados
 * en {@code RequestLoggingContextFilter} para toda request — ver
 * {@link com.vetsoftware.app.infrastructure.logging.MdcKeys}) viajan por el MDC, y el
 * encoder estructurado de Spring Boot los emite como campos JSON; aquí solo se añaden los campos
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
    private final AuditEventStore eventStore;

    public AuditLogger(AuditEventStore eventStore) {
        this.eventStore = eventStore;
    }

    /** Mutación de recurso en el borde HTTP (POST/PUT/PATCH/DELETE). http.method/http.path vía MDC. */
    public void mutation(String method, String path, int status, String outcome, long durationMs) {
        persist("http_mutation", outcome,
                "http.method", method,
                "http.path", path,
                "http.status", status,
                "http.durationMs", durationMs);
        audit.atInfo()
                .addKeyValue("event", "http_mutation")
                .addKeyValue("http.status", status)
                .addKeyValue("outcome", outcome)
                .addKeyValue("http.durationMs", durationMs)
                .log("mutation {} {} -> {} ({})", method, path, status, outcome);
    }

    /** Alta de una nueva empresa (veterinaria) por auto-registro público. Sin datos sensibles;
     *  {@code ownerCode} es el código de acceso del dueño, no un secreto. client.ip/http.path vía MDC. */
    public void companyRegistered(Long companyId, String companyName, String companyIdentifier,
                                  Long ownerEmployeeId, String ownerCode) {
        persist("company_registered", "SUCCESS",
                "company.id", companyId,
                "company.name", companyName,
                "company.identifier", companyIdentifier,
                "actor.employeeId", ownerEmployeeId,
                "actor.identifier", ownerCode);
        audit.atInfo()
                .addKeyValue("event", "company_registered")
                .addKeyValue("company.id", companyId)
                .addKeyValue("company.name", companyName)
                .addKeyValue("company.identifier", companyIdentifier)
                .addKeyValue("actor.employeeId", ownerEmployeeId)
                .addKeyValue("actor.identifier", ownerCode)
                .addKeyValue("outcome", "SUCCESS")
                .log("company registered id={} name={}", companyId, companyName);
    }

    /** Un admin (o quien tenga el permiso) envió una invitación de acceso a un nuevo empleado. El actor que
     *  invita viaja en el MDC (actor.employeeId / actor.companyId). {@code invitedCode} es el código de
     *  acceso del invitado, no un secreto. */
    public void employeeInvited(Long invitedEmployeeId, String invitedCode, Long companyId) {
        persist("employee_invited", "SUCCESS",
                "employee.id", invitedEmployeeId,
                "employee.identifier", invitedCode,
                "company.id", companyId);
        audit.atInfo()
                .addKeyValue("event", "employee_invited")
                .addKeyValue("employee.id", invitedEmployeeId)
                .addKeyValue("employee.identifier", invitedCode)
                .addKeyValue("company.id", companyId)
                .addKeyValue("outcome", "SUCCESS")
                .log("employee invited id={} code={}", invitedEmployeeId, invitedCode);
    }

    /** Reenvío de la invitación (nueva contraseña provisional) por un admin/con permiso. Actor vía MDC. */
    public void employeeInvitationResent(Long invitedEmployeeId, String invitedCode, Long companyId) {
        persist("employee_invitation_resent", "SUCCESS",
                "employee.id", invitedEmployeeId,
                "employee.identifier", invitedCode,
                "company.id", companyId);
        audit.atInfo()
                .addKeyValue("event", "employee_invitation_resent")
                .addKeyValue("employee.id", invitedEmployeeId)
                .addKeyValue("employee.identifier", invitedCode)
                .addKeyValue("company.id", companyId)
                .addKeyValue("outcome", "SUCCESS")
                .log("employee invitation resent id={} code={}", invitedEmployeeId, invitedCode);
    }

    /** El empleado invitado aceptó la invitación: completó su primer ingreso cambiando la contraseña
     *  temporal (INVITED → activo con contraseña propia). El propio empleado es el actor (MDC). */
    public void invitationAccepted(Long employeeId, Long companyId) {
        persist("invitation_accepted", "SUCCESS",
                "employee.id", employeeId,
                "company.id", companyId);
        audit.atInfo()
                .addKeyValue("event", "invitation_accepted")
                .addKeyValue("employee.id", employeeId)
                .addKeyValue("company.id", companyId)
                .addKeyValue("outcome", "SUCCESS")
                .log("invitation accepted employeeId={}", employeeId);
    }

    /** Login exitoso; {@code identifier} es el código de empleado/usuario, no un secreto. */
    public void loginSuccess(String userType, String identifier) {
        persist("login_success", "SUCCESS",
                "actor.type", userType,
                "actor.identifier", identifier);
        audit.atInfo()
                .addKeyValue("event", "login_success")
                .addKeyValue("actor.type", userType)
                .addKeyValue("actor.identifier", identifier)
                .addKeyValue("outcome", "SUCCESS")
                .log("login success type={} id={}", userType, identifier);
    }

    /** Intento de login fallido; sin identificador (no disponible en el handler) ni credenciales.
     *  http.path vía MDC. */
    public void loginFailure(String path, String reason) {
        persist("login_failure", "FAILURE",
                "http.path", path,
                "reason", reason);
        audit.atWarn()
                .addKeyValue("event", "login_failure")
                .addKeyValue("outcome", "FAILURE")
                .addKeyValue("reason", reason)
                .log("login failure {} reason={}", path, reason);
    }

    /** Login bloqueado porque el correo del empleado aún no está verificado (auto-registro Opción B).
     *  {@code identifier} es el código de empleado intentado (no un secreto). http.path/client.ip vía MDC. */
    public void loginBlockedEmailNotVerified(String identifier) {
        persist("login_blocked_email_not_verified", "DENIED",
                "actor.identifier", identifier,
                "reason", "email_not_verified");
        audit.atWarn()
                .addKeyValue("event", "login_blocked_email_not_verified")
                .addKeyValue("actor.identifier", identifier)
                .addKeyValue("outcome", "DENIED")
                .addKeyValue("reason", "email_not_verified")
                .log("login blocked, email not verified id={}", identifier);
    }

    /** Denegación de autorización (@PreAuthorize → AccessDeniedException). Actor, http.* vía MDC. */
    public void accessDenied(String method, String path) {
        persist("access_denied", "DENIED",
                "http.method", method,
                "http.path", path);
        audit.atWarn()
                .addKeyValue("event", "access_denied")
                .addKeyValue("outcome", "DENIED")
                .log("access denied {} {}", method, path);
    }

    /** Acceso a un recurso protegido sin autenticación válida (token ausente/inválido → 401).
     *  http.method/http.path vía MDC. */
    public void unauthenticated(String method, String path, String reason) {
        persist("unauthenticated", "DENIED",
                "http.method", method,
                "http.path", path,
                "reason", reason);
        audit.atWarn()
                .addKeyValue("event", "unauthenticated")
                .addKeyValue("outcome", "DENIED")
                .addKeyValue("reason", reason)
                .log("unauthenticated {} {} reason={}", method, path, reason);
    }

    /** Login bloqueado por rate limiting (429) — señal de fuerza bruta. client.ip/http.path vía MDC. */
    public void loginRateLimited() {
        rateLimited("LOGIN_RATE_LIMITED");
    }

    /** Ruta publica bloqueada por rate limiting (429). client.ip/http.path via MDC. */
    public void rateLimited(String code) {
        persist("rate_limited", "DENIED", "code", code);
        audit.atWarn()
                .addKeyValue("event", "rate_limited")
                .addKeyValue("code", code)
                .addKeyValue("outcome", "DENIED")
                .log("rate limited code={}", code);
    }

    private void persist(String eventType, String outcome, Object... keyValues) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            attributes.put((String) keyValues[index], keyValues[index + 1]);
        }
        eventStore.append(eventType, outcome, attributes);
    }
}
