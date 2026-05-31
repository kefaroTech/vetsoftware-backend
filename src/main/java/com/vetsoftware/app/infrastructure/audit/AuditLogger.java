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
 * ({@code client.ip} / {@code http.method} / {@code http.path}, poblados en
 * {@code TraceContextResetFilter} para toda request — ver
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
        audit.warn("login rate limited",
                kv("event", "login_rate_limited"),
                kv("outcome", "DENIED"));
    }
}
