package com.vetsoftware.app.aiproposal.infrastructure.ai;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * El vocabulario cerrado de {@code error.type} de la llamada al modelo, y la
 * particion que <strong>decide el nivel de log</strong>.
 *
 * <p>
 * &#9940; <strong>Lo que decide el nivel no es la gravedad: es si el mismo
 * intento podria salir bien despues sin que nadie toque nada.</strong> Un
 * {@code catch} con un solo {@code log.warn} mete dos poblaciones distintas en
 * la misma rama: un 429 falla peticiones aisladas y se cura solo, y un
 * {@code AccessDeniedException} falla el <strong>100 %</strong> de las
 * propuestas hasta que una persona cambie IAM o habilite el modelo en la
 * region. Con un solo nivel, el fallo total se esconde detras del ruido del
 * fallo aislado, que es exactamente lo que pasa a las tres de la manana cuando
 * el producto lleva seis horas vendiendo sin IA y {@code level=error} esta
 * limpio.
 *
 * <p>
 * <strong>{@link #OTHER} es sistemico a proposito.</strong> Un codigo sin rama
 * es una rama que falta, y que ese contador crezca <em>es</em> la senal. Lo
 * contrario -tratar lo desconocido como transitorio- convierte cada codigo
 * nuevo del proveedor en ruido de fondo que nadie mira.
 *
 * <p>
 * <strong>Las ocho ramas del proveedor todavia no las emite nadie.</strong> Hoy
 * el unico invocador desplegado es {@link BedrockDisabledInvoker} y el unico
 * codigo real es {@link #MODEL_ACCESS_NOT_ENABLED}. Estan escritas igual porque
 * son el contrato que tiene que cumplir el cliente de Bedrock cuando entre: si
 * devuelve {@code THROTTLING_EXCEPTION} en vez de {@code MODEL_RATE_LIMITED},
 * cae en {@link #OTHER}, sube el contador de sistemicos y alguien lo ve. Un
 * vocabulario que falla hacia el lado ruidoso es un vocabulario que se arregla;
 * uno que falla hacia el lado silencioso, no.
 */
public enum AiErrorType {

    /** El camino feliz. La etiqueta existe tambien cuando no hubo error. */
    NONE("none", false),

    MODEL_TIMEOUT("timeout", false),

    MODEL_CONNECTION_ERROR("connection_error", false),

    MODEL_RATE_LIMITED("rate_limited", false),

    MODEL_SERVER_ERROR("server_error", false),

    MODEL_OVERLOADED("overloaded", false),

    /**
     * La respuesta no es un JSON utilizable. Aislado y no sistemico: lo produce un
     * modelo con el texto de un tercero en contexto, asi que una salida rota es un
     * caso normal de una peticion, no una averia de la configuracion. Si pasa a ser
     * la mayoria, quien despierta a alguien es la alerta sobre la tasa, no el nivel
     * del evento suelto.
     */
    MODEL_OUTPUT_UNREADABLE("output_unreadable", false),

    /**
     * ⛔ <strong>El modelo configurado no honra el mecanismo de salida
     * estructurada.</strong> Se pidio la herramienta de
     * {@code ProposalOutputSchema} con {@code toolChoice} forzado y la respuesta no
     * trajo su bloque. <strong>Sistemico, y esa es toda la razon de que exista una
     * rama propia</strong>: el uso de herramientas es una capacidad por modelo, asi
     * que un modelo que la ignora la va a ignorar en el 100 % de las propuestas
     * hasta que alguien mueva {@code structured-output}. Sin esta rama caeria en
     * {@code MODEL_OUTPUT_UNREADABLE}, que es <em>aislado</em> y se escribe con
     * {@code WARN}: el dia que se cambiara de familia de modelo, la averia total se
     * esconderia detras del ruido normal y el panel diria «algunas respuestas salen
     * mal» durante semanas.
     */
    MODEL_STRUCTURED_OUTPUT_UNSUPPORTED("structured_output_unsupported", true),

    MODEL_UNAUTHORIZED("unauthorized", true),

    MODEL_FORBIDDEN("forbidden", true),

    MODEL_INVALID_REQUEST("invalid_request", true),

    /**
     * El acceso al modelo no esta habilitado en la cuenta de AWS. Sistemico por
     * definicion: depende de un formulario manual en la consola.
     */
    MODEL_ACCESS_NOT_ENABLED("model_access_not_enabled", true),

    /**
     * Cualquier {@code RuntimeException} que el invocador deje escapar sin
     * clasificar. Sistemico: no sabemos si se cura solo, y lo desconocido se trata
     * como lo peor.
     */
    MODEL_UNEXPECTED_ERROR("unexpected_error", true),

    /** Sin clasificar: falta una rama, y que crezca es la senal. */
    OTHER("_other", true);

    /**
     * Guardarrail de inyeccion en el log (ASVS V7.3.1) sobre el {@code failureCode}
     * crudo: el contrato dice que es vocabulario cerrado, pero lo cumple quien
     * implemente {@link ModelInvoker} y no nosotros. Lo que no case sale como
     * {@link #DESCONOCIDO}, que no puede fabricar una linea de log falsa ni
     * arrastrar el cuerpo de la peticion -y el cuerpo lleva el texto del
     * prospecto-.
     */
    private static final Pattern CODIGO_SEGURO = Pattern.compile("[A-Z][A-Z0-9_]{0,39}");

    /** Sustituto de un {@code failureCode} que no tiene la forma acordada. */
    public static final String DESCONOCIDO = "_OTHER";

    private final String value;

    private final boolean sistemico;

    AiErrorType(String value, boolean sistemico) {
        this.value = value;
        this.sistemico = sistemico;
    }

    /** El valor tal como viaja en la etiqueta {@code error.type}. */
    public String value() {
        return value;
    }

    /**
     * {@code true} si el fallo va a repetirse en el 100 % de las propuestas hasta
     * que una persona cambie configuracion. Es lo que separa {@code ERROR} de
     * {@code WARN}, y lo unico que lo separa.
     */
    public boolean esSistemico() {
        return sistemico;
    }

    /**
     * Traduce el {@code failureCode} del invocador. Insensible a la caja porque un
     * cliente que devuelva {@code forbidden} en minusculas describe el mismo hecho;
     * lo que no case con ninguna constante cae en {@link #OTHER}.
     */
    public static AiErrorType deFailureCode(String failureCode) {
        if (failureCode == null || failureCode.isBlank()) {
            return OTHER;
        }
        String normalizado = failureCode.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(tipo -> tipo.name().equals(normalizado)).findFirst()
                .orElse(OTHER);
    }

    /**
     * El {@code failureCode} en la forma que se puede escribir en un log. Ver
     * {@link #CODIGO_SEGURO}.
     */
    public static String codigoSeguro(String failureCode) {
        if (failureCode == null) {
            return DESCONOCIDO;
        }
        String normalizado = failureCode.trim().toUpperCase(Locale.ROOT);
        return CODIGO_SEGURO.matcher(normalizado).matches() ? normalizado : DESCONOCIDO;
    }
}
