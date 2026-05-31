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
 * {@code actor.systemUserId}, ver {@link com.vetsoftware.app.infrastructure.logging.MdcKeys})
 * viajan por el MDC (poblado en {@code AuthFilter}) y el {@code LogstashEncoder} los emite
 * automáticamente como campos JSON; aquí solo se añaden los campos propios del evento.
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

    /** Mutación de recurso en el borde HTTP (POST/PUT/PATCH/DELETE). */
    public void mutation(String method, String path, int status, String outcome, long durationMs) {
        audit.info("mutation {} {} -> {} ({})", method, path, status, outcome,
                kv("event", "http_mutation"),
                kv("http.method", method),
                kv("http.path", path),
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

    /** Intento de login fallido; sin identificador (no disponible en el handler) ni credenciales. */
    public void loginFailure(String path, String reason) {
        audit.warn("login failure {} reason={}", path, reason,
                kv("event", "login_failure"),
                kv("http.path", path),
                kv("outcome", "FAILURE"),
                kv("reason", reason));
    }

    /** Denegación de autorización (@PreAuthorize → AccessDeniedException). Actor vía MDC. */
    public void accessDenied(String method, String path) {
        audit.warn("access denied {} {}", method, path,
                kv("event", "access_denied"),
                kv("http.method", method),
                kv("http.path", path),
                kv("outcome", "DENIED"));
    }

    /** Acceso a un recurso protegido sin autenticación válida (token ausente/inválido → 401). */
    public void unauthenticated(String method, String path, String reason) {
        audit.warn("unauthenticated {} {} reason={}", method, path, reason,
                kv("event", "unauthenticated"),
                kv("http.method", method),
                kv("http.path", path),
                kv("outcome", "DENIED"),
                kv("reason", reason));
    }
}
