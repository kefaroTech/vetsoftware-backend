package com.vetsoftware.app.infrastructure.logging;

import java.util.Set;

/**
 * Allowlist de campos estructurados de log (MDC y {@code KeyValuePair}).
 *
 * <p>Es una <b>allowlist</b>, no una denylist: una clave que no aparezca aquí ve su valor
 * reemplazado por {@link LogRedactor#MASK} antes de salir del proceso. Añadir un campo nuevo al MDC
 * o un {@code addKeyValue(...)} nuevo obliga a declararlo en esta clase — es el punto de gobierno
 * donde se decide, una sola vez y de forma revisable, que un campo es apto para archivos y Loki.
 *
 * <p>Dos niveles:
 * <ul>
 *   <li>{@link #VERBATIM} — el valor sale tal cual. Reservado para identificadores técnicos y
 *       códigos de negocio de forma conocida y acotada (ids numéricos, enums de resultado,
 *       traceId/spanId, IP de origen). No se les aplica enmascarado de texto porque su forma es
 *       segura y el enmascarado los dañaría (p.ej. un NIT de 10 dígitos sería confundido con un
 *       documento personal).</li>
 *   <li>{@link #SCANNED} — el valor se permite pero pasa por {@link LogRedactor#redact(String)}.
 *       Para campos de texto libre o semi-libre donde puede colarse un correo, un teléfono o un
 *       token embebido (nombre comercial, ruta HTTP, User-Agent).</li>
 * </ul>
 *
 * <p>El único cruce con {@link MdcKeys} es intencional: esta clase declara la política de salida de
 * esas mismas claves más las de los eventos {@code AUDIT}
 * ({@link com.vetsoftware.app.infrastructure.audit.AuditLogger}).
 *
 * @see LogRedactor
 * @see RedactingAppender
 */
public final class LogFieldPolicy {

    private LogFieldPolicy() {}

    /**
     * Claves cuyo valor se emite sin tocar.
     *
     * <p>{@code traceId}/{@code spanId}/{@code traceFlags}/{@code sampled} son propiedad de
     * Micrometer Tracing. {@code company.identifier} es el NIT de la <em>empresa cliente</em>
     * (dato mercantil público del tenant, auditado a propósito), no el documento de una persona.
     * {@code actor.identifier} y {@code employee.identifier} son códigos de acceso de empleado, que
     * {@code AuditLogger} documenta explícitamente como no secretos.
     */
    private static final Set<String> VERBATIM = Set.of(
            // Correlación — Micrometer Tracing
            "traceId", "spanId", "traceFlags", "sampled", "trace_id", "span_id",
            // Contexto de request y actor — MdcKeys
            MdcKeys.ACTOR_TYPE,
            MdcKeys.ACTOR_EMPLOYEE_ID,
            MdcKeys.ACTOR_COMPANY_ID,
            MdcKeys.ACTOR_SYSTEM_USER_ID,
            MdcKeys.CLIENT_IP,
            MdcKeys.HTTP_METHOD,
            // Campos propios de los eventos AUDIT
            "event", "outcome", "reason", "code",
            "http.status", "http.durationMs",
            "company.id", "company.identifier",
            "employee.id", "employee.identifier",
            "actor.identifier");

    /** Claves permitidas cuyo valor sí se somete al enmascarado de texto. */
    private static final Set<String> SCANNED = Set.of(
            MdcKeys.HTTP_PATH,
            MdcKeys.USER_AGENT,
            "company.name");

    /** {@code true} si el valor de {@code key} se emite sin transformación alguna. */
    public static boolean isVerbatim(String key) {
        return key != null && VERBATIM.contains(key);
    }

    /** {@code true} si {@code key} está permitida pero su valor debe pasar por el enmascarado. */
    public static boolean isScanned(String key) {
        return key != null && SCANNED.contains(key);
    }

    /** {@code true} si {@code key} está declarada en la allowlist en cualquiera de los dos niveles. */
    public static boolean isAllowed(String key) {
        return isVerbatim(key) || isScanned(key);
    }
}
