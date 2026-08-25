package com.vetsoftware.app.infrastructure.logging;

import java.util.Set;

/**
 * Allowlist de campos estructurados de log (MDC y {@code KeyValuePair}).
 *
 * <p>
 * Es una <b>allowlist</b>, no una denylist: una clave que no aparezca aquí ve
 * su valor reemplazado por {@link LogRedactor#MASK} antes de salir del proceso.
 * Añadir un campo nuevo al MDC o un {@code addKeyValue(...)} nuevo obliga a
 * declararlo en esta clase — es el punto de gobierno donde se decide, una sola
 * vez y de forma revisable, que un campo es apto para archivos y Loki.
 *
 * <p>
 * Dos niveles:
 *
 * <ul>
 * <li>{@link #VERBATIM} — el valor sale tal cual. Reservado para
 * identificadores técnicos y códigos de negocio de forma conocida y acotada
 * (ids numéricos, enums de resultado, traceId/spanId, IP de origen). No se les
 * aplica enmascarado de texto porque su forma es segura y el enmascarado los
 * dañaría (p.ej. un NIT de 10 dígitos sería confundido con un documento
 * personal).
 * <li>{@link #SCANNED} — el valor se permite pero pasa por
 * {@link LogRedactor#redact(String)}. Para campos de texto libre o semi-libre
 * donde puede colarse un correo, un teléfono o un token embebido (nombre
 * comercial, ruta HTTP, User-Agent) y para todo identificador <b>cuya forma la
 * elige el usuario</b> y no el sistema: el código de acceso de un empleado es
 * una cadena arbitraria, y en el auto-registro resulta ser su correo.
 * </ul>
 *
 * <p>
 * De ahí el criterio para colocar una clave: no basta con que el campo «no sea
 * un secreto» —{@code VERBATIM} no es la lista de lo no confidencial, es la de
 * los valores cuya <b>forma</b> el sistema garantiza— y un valor que teclea un
 * humano nunca la tiene.
 *
 * <p>
 * El único cruce con {@link MdcKeys} es intencional: esta clase declara la
 * política de salida de esas mismas claves más las de los eventos {@code AUDIT}
 * ({@link com.vetsoftware.app.infrastructure.audit.AuditLogger}).
 *
 * @see LogRedactor
 * @see RedactingAppender
 */
public final class LogFieldPolicy {

    private LogFieldPolicy() {
    }

    /**
     * Claves cuyo valor se emite sin tocar.
     *
     * <p>
     * {@code traceId}/{@code spanId}/{@code traceFlags}/{@code sampled} son
     * propiedad de Micrometer Tracing. {@code company.identifier} es el NIT de la
     * <em>empresa cliente</em> (dato mercantil público del tenant, auditado a
     * propósito), no el documento de una persona. {@code actor.id},
     * {@code employee.id} y {@code seconds_since_revocation} son ids numéricos y
     * una duración en segundos: su tipo ya acota su forma, y someterlos al
     * enmascarado de texto solo podría mutilarlos — un id largo se confundiría con
     * un documento personal, igual que el NIT.
     *
     * <p>
     * <b>Lo que estaba aquí y dejó de estarlo:</b> {@code actor.identifier} y
     * {@code employee.identifier} (incidencia #216). El motivo, en
     * {@link #SCANNED}.
     */
    private static final Set<String> VERBATIM = Set.of(
            // Correlación — Micrometer Tracing
            "traceId", "spanId", "traceFlags", "sampled", "trace_id", "span_id",
            // Contexto de request y actor — MdcKeys
            MdcKeys.ACTOR_TYPE, MdcKeys.ACTOR_EMPLOYEE_ID, MdcKeys.ACTOR_COMPANY_ID,
            MdcKeys.ACTOR_SYSTEM_USER_ID, MdcKeys.CLIENT_IP, MdcKeys.HTTP_METHOD,
            MdcKeys.SYSTEM_USER_REQUEST_ID,
            // Campos propios de los eventos AUDIT
            "event", "outcome", "reason", "code", "http.status", "http.durationMs", "company.id",
            "company.identifier", "employee.id", "actor.id", "seconds_since_revocation",
            // Alta de superadministradores de plataforma: un contador pequeño de
            // intentos restantes y una antigüedad en segundos, hermana de
            // seconds_since_revocation. Los dos son números que produce el sistema, no
            // texto que teclea nadie.
            "attempts.remaining", "seconds_since_consumption");

    /**
     * Claves permitidas cuyo valor sí se somete al enmascarado de texto.
     *
     * <p>
     * <b>{@code actor.identifier} y {@code employee.identifier} viven aquí, no en
     * {@link #VERBATIM}</b> (incidencia #216). La premisa que los declaraba
     * verbatim —«son códigos de acceso de empleado, y un código de acceso no es un
     * dato personal»— es falsa en este producto: en el auto-registro el código de
     * acceso <b>es</b> el correo del dueño ({@code RegisterUserService.register},
     * {@code String employeeCode =
     * command.employeeEmail().trim()}), así que cada alta de veterinaria
     * ({@code company_registered}) y cada login ({@code login_success}) publicaban
     * el correo del usuario en claro hasta Loki.
     *
     * <p>
     * <b>Por qué el arreglo va aquí y no en cada emisor.</b> La incidencia #180 ya
     * había tapado uno de los tres puntos —{@code GlobalExceptionHandler} redacta
     * el identificador antes de entregárselo a {@code AuditLogger}— y los otros dos
     * siguieron filtrando durante meses. Mientras la política diga «verbatim»,
     * protegerse es opcional y quien escriba el cuarto emisor no tiene por qué
     * enterarse: el defecto vuelve con la suite en verde. Escanear la clave lo
     * cierra de una vez para todo emisor presente y futuro, y deja la redacción
     * manual de #180 como redundante e inofensiva, porque el enmascarado es
     * idempotente.
     *
     * <p>
     * <b>No ciega la investigación.</b> El enmascarado es por patrones: un código
     * que no sea un correo ni un documento —{@code EMP0042}, {@code OWNER01}— no
     * casa con ninguno y sale entero, igual que antes. Un correo sale como
     * {@code ***@clinica.com}, conservando el dominio, que es la misma forma que
     * #180 ya dio por buena; y un código que sea una cédula queda suprimido, que es
     * exactamente lo que debe pasarle a un documento personal.
     */
    private static final Set<String> SCANNED = Set.of(MdcKeys.HTTP_PATH, MdcKeys.USER_AGENT,
            "company.name", "actor.identifier", "employee.identifier", "email.domain");

    /**
     * {@code true} si el valor de {@code key} se emite sin transformación alguna.
     */
    public static boolean isVerbatim(String key) {
        return key != null && VERBATIM.contains(key);
    }

    /**
     * {@code true} si {@code key} está permitida pero su valor debe pasar por el
     * enmascarado.
     */
    public static boolean isScanned(String key) {
        return key != null && SCANNED.contains(key);
    }

    /**
     * {@code true} si {@code key} está declarada en la allowlist en cualquiera de
     * los dos niveles.
     */
    public static boolean isAllowed(String key) {
        return isVerbatim(key) || isScanned(key);
    }
}
