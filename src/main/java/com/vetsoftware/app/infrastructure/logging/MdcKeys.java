package com.vetsoftware.app.infrastructure.logging;

/**
 * Claves MDC del actor, fuente única de verdad para que el productor ({@code AuthFilter}) y la
 * limpieza defensiva ({@code TraceContextResetFilter}) usen exactamente los mismos nombres.
 *
 * <p>Notación con punto, alineada con los eventos {@code AUDIT} y las semantic conventions de
 * OpenTelemetry. El {@code LogstashEncoder} las emite como campos JSON.
 */
public final class MdcKeys {

    private MdcKeys() {}

    public static final String ACTOR_TYPE = "actor.type";
    public static final String ACTOR_EMPLOYEE_ID = "actor.employeeId";
    public static final String ACTOR_COMPANY_ID = "actor.companyId";
    public static final String ACTOR_SYSTEM_USER_ID = "actor.systemUserId";

    /** IP de origen de la request (OWASP ASVS 7.1.4 "source of the event"). */
    public static final String CLIENT_IP = "client.ip";

    /**
     * User-Agent de la request (semantic convention de OTel {@code user_agent.original}). Complementa
     * a {@code client.ip} para fingerprinting de abuso/fuerza bruta. Puede faltar (header opcional).
     */
    public static final String USER_AGENT = "user_agent.original";

    /** Método y ruta HTTP de la request en curso → logs de error autocontenidos. */
    public static final String HTTP_METHOD = "http.method";
    public static final String HTTP_PATH = "http.path";
}
