package com.vetsoftware.app.infrastructure.email;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.web.client.RestClientResponseException;

/**
 * Vocabulario <b>cerrado</b> con el que {@link ResendEmailClient} clasifica un
 * fallo de envío. Es la etiqueta {@code error.type} de las semantic conventions
 * de OpenTelemetry: un conjunto pequeño y fijo, no el nombre de la clase de
 * excepción.
 *
 * <p>
 * <b>Por qué no vale el nombre de la excepción.</b> El manejador de
 * observaciones de Micrometer etiqueta el medidor con
 * {@code error = <simple name de la excepción>}, así que cada subclase de
 * {@link RestClientResponseException} —una por estado HTTP— y cada
 * {@link IOException} del transporte estrenan una serie temporal nueva. Eso es
 * cardinalidad no acotada, y en {@code email.send*} nadie la filtra:
 * {@code BusinessMetricCardinalityFilter} solo mira el prefijo
 * {@code vetsoftware.business.}. El corte lo hace
 * {@link EmailMetricCardinalityFilter}, que colapsa lo que no esté aquí
 * declarado.
 *
 * <p>
 * <b>Transitorio contra determinista</b> ({@link #transitory()}). No es un
 * matiz de redacción: decide el nivel del registro. Un 429 o un 503 se curan
 * solos y el envío siguiente puede salir —{@code WARN}—; un 403 por dominio sin
 * verificar o un 401 por API key inválida no se arreglan reintentando y
 * necesitan que una persona cambie configuración —{@code ERROR}, con el mismo
 * criterio de «fallo terminal» que usa
 * {@code BusinessMetricCardinalityFilter}—. Hoy los cinco flujos de correo
 * pierden el mensaje en los dos casos (no hay reintento ni outbox), pero solo
 * uno de los dos justifica despertar a alguien.
 */
enum EmailErrorType {

    /**
     * No hubo fallo. Se emite en el camino feliz para que la etiqueta exista
     * siempre.
     */
    NONE("none", false),

    /** Se agotó el tiempo de espera contra Resend (o HTTP 408). */
    TIMEOUT("timeout", true),

    /** La conexión no llegó a completarse: DNS, rechazo, corte de red. */
    CONNECTION("connection_error", true),

    /** HTTP 429: se superó el límite de tasa del proveedor. */
    RATE_LIMITED("rate_limited", true),

    /** HTTP 5xx: avería del lado de Resend. */
    SERVER_ERROR("server_error", true),

    /** HTTP 401: la API key falta, caducó o no es válida. */
    UNAUTHORIZED("unauthorized", false),

    /**
     * HTTP 403: la cuenta no puede enviar así (p. ej. dominio remitente sin
     * verificar).
     */
    FORBIDDEN("forbidden", false),

    /** Resto de 4xx (400, 422…): la petición que construimos no es aceptable. */
    INVALID_REQUEST("invalid_request", false),

    /**
     * Cubo de reserva de las semantic conventions para lo que no sabemos
     * clasificar. Que crezca es la señal de que falta una rama en {@link #of}.
     */
    UNKNOWN("_OTHER", false);

    /** Tope de saltos al recorrer la cadena de causas; la protege de ciclos. */
    private static final int MAX_CAUSE_DEPTH = 10;

    private static final Set<String> TAGS = Stream.of(values()).map(EmailErrorType::tag)
            .collect(Collectors.toUnmodifiableSet());

    private final String tag;
    private final boolean transitory;

    EmailErrorType(String tag, boolean transitory) {
        this.tag = tag;
        this.transitory = transitory;
    }

    /** Valor tal como viaja en la etiqueta {@code error.type}. */
    String tag() {
        return tag;
    }

    /**
     * {@code true} si el mismo envío podría salir en un intento posterior sin que
     * nadie toque nada.
     */
    boolean transitory() {
        return transitory;
    }

    /**
     * El conjunto cerrado completo, para la lista blanca del filtro de medidores.
     */
    static Set<String> tags() {
        return TAGS;
    }

    static EmailErrorType of(Throwable error) {
        if (error == null) {
            return NONE;
        }
        if (error instanceof RestClientResponseException response) {
            return ofStatus(response.getStatusCode().value());
        }
        return ofTransport(error);
    }

    private static EmailErrorType ofStatus(int status) {
        if (status == 408) {
            return TIMEOUT;
        }
        if (status == 429) {
            return RATE_LIMITED;
        }
        if (status >= 500 && status < 600) {
            return SERVER_ERROR;
        }
        if (status == 401) {
            return UNAUTHORIZED;
        }
        if (status == 403) {
            return FORBIDDEN;
        }
        if (status >= 400 && status < 500) {
            return INVALID_REQUEST;
        }
        return UNKNOWN;
    }

    /**
     * El fallo de transporte llega envuelto —{@code ResourceAccessException} sobre
     * la {@link IOException} real—, así que la clasificación mira la cadena de
     * causas y no solo la excepción de arriba.
     */
    private static EmailErrorType ofTransport(Throwable error) {
        boolean networkFailure = false;
        Throwable cause = error;
        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException
                    || cause instanceof TimeoutException) {
                return TIMEOUT;
            }
            networkFailure = networkFailure || cause instanceof IOException;
            cause = cause.getCause();
        }
        return networkFailure ? CONNECTION : UNKNOWN;
    }
}
