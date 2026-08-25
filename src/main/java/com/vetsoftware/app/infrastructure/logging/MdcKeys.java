package com.vetsoftware.app.infrastructure.logging;

/**
 * Claves MDC propiedad de VetSoftware, fuente única de verdad para productores
 * y limpieza.
 *
 * <p>
 * Notación con punto, alineada con los eventos {@code AUDIT} y las semantic
 * conventions de OpenTelemetry. Spring Boot las emite como campos del log JSON
 * estructurado.
 *
 * <p>
 * {@code traceId} y {@code spanId} no se declaran aquí: Micrometer Tracing es
 * su único dueño y administra automáticamente su alta, restauración y limpieza
 * en el MDC.
 *
 * <p>
 * <b>Una clave nueva debe declararse también en {@link LogFieldPolicy}.</b> El
 * MDC se emite bajo allowlist: lo que no esté declarado allí sale como
 * {@code ***}. Es deliberado — un campo nuevo es opaco por defecto — pero
 * significa que añadir una constante aquí sin declarar su política deja el
 * valor invisible en Grafana. Ver {@code docs/POLITICA_REDACCION_LOGS.md}.
 */
public final class MdcKeys {

    private MdcKeys() {
    }

    public static final String ACTOR_TYPE = "actor.type";
    public static final String ACTOR_EMPLOYEE_ID = "actor.employeeId";
    public static final String ACTOR_COMPANY_ID = "actor.companyId";
    public static final String ACTOR_SYSTEM_USER_ID = "actor.systemUserId";

    /** IP de origen de la request (OWASP ASVS 7.1.4 "source of the event"). */
    public static final String CLIENT_IP = "client.ip";

    /**
     * User-Agent de la request (semantic convention de OTel
     * {@code user_agent.original}). Complementa a {@code client.ip} para
     * fingerprinting de abuso/fuerza bruta. Puede faltar (header opcional).
     */
    public static final String USER_AGENT = "user_agent.original";

    /** Método y ruta HTTP de la request en curso → logs de error autocontenidos. */
    public static final String HTTP_METHOD = "http.method";

    public static final String HTTP_PATH = "http.path";

    /**
     * Id de la solicitud de alta de superadministrador de plataforma. Ata las tres
     * peticiones del flujo —solicitud, aprobación e invitación aceptada—, que están
     * separadas por horas o días y por eso <b>no</b> comparten {@code traceId}: W3C
     * Trace Context identifica una operación distribuida, no un proceso de negocio
     * con un humano dentro.
     *
     * <p>
     * Es el {@code Long} de la clave primaria, como {@code company.id} o
     * {@code employee.id}: su forma la garantiza el sistema, así que va
     * {@code VERBATIM} en {@link LogFieldPolicy}. En Loki, tras {@code | json}, se
     * consulta como {@code system_user_request_id}.
     *
     * <p>
     * <b>Se declara en cuatro sitios y hay que poner los cuatro</b> (contrato en
     * {@code docs/TELEMETRIA_ALTA_SUPERADMIN.md} §3.2): aquí,
     * {@link LogFieldPolicy}, {@code
     * RequestLoggingContextFilter.clearApplicationContext()} y la lista explícita
     * de {@code AsyncConfig.contextPropagatingTaskDecorator()}. Faltar cualquiera
     * rompe la correlación <b>en silencio</b>: sin error, sin alerta, solo huecos.
     */
    public static final String SYSTEM_USER_REQUEST_ID = "system.user.request.id";
}
