package com.vetsoftware.app.infrastructure.email;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Acota las etiquetas de los medidores {@code email.send} y
 * {@code email.send.template}. Existe porque
 * {@code BusinessMetricCardinalityFilter} <b>no los mira</b>: ese filtro solo
 * actúa dentro de {@code vetsoftware.business.}, y estos dos nombres quedan
 * fuera del prefijo de negocio. Hasta hoy no se filtraban ni dejaban rastro de
 * no filtrarse.
 *
 * <p>
 * <b>Qué se acota y por qué.</b> El manejador de observaciones de Micrometer
 * añade por su cuenta {@code error = <simple name de la excepción>}. Ese valor
 * no lo elige este código y no está acotado: una serie por subclase de
 * {@code RestClientResponseException} —hay una por estado HTTP— y otra por cada
 * excepción de red. En producción se observó {@code error="Forbidden"}, que es
 * solo el primer valor de una lista que crece sola.
 *
 * <p>
 * <b>Se colapsa, no se descarta</b>, al revés que en el filtro de negocio. Un
 * descarte dejaría el panel de correo ciego justo durante una avería del
 * proveedor, que es cuando hace falta. Y {@code error} se colapsa a
 * {@code none} / {@value #OTHER} en vez de eliminarse para que las consultas
 * existentes del tipo {@code error != "none"} sigan diciendo la verdad; el
 * detalle acotado vive ahora en {@code error.type}, que emite
 * {@link ResendEmailClient} con el vocabulario cerrado de
 * {@link EmailErrorType}.
 *
 * <p>
 * Un valor inesperado en {@code error.type} o en {@code email.outcome} sí deja
 * rastro: significa que alguien añadió un desenlace nuevo al cliente y olvidó
 * declararlo aquí, y sin el aviso el valor se fundiría en {@value #OTHER} en
 * silencio. Se registra una vez por par etiqueta/valor y con tope, porque lo
 * inesperado es justamente lo que puede llegar en tromba.
 */
@Component
public class EmailMetricCardinalityFilter implements MeterFilter {

    /** Cubre {@code email.send}, {@code email.send.template} y sus derivados. */
    static final String PREFIX = "email.send";

    /** Cubo de reserva de las semantic conventions de OpenTelemetry. */
    static final String OTHER = "_OTHER";

    /** Etiqueta que pone Micrometer con el nombre de la clase de excepción. */
    static final String ERROR_TAG = "error";

    /** Etiqueta acotada que emite {@link ResendEmailClient}. */
    static final String ERROR_TYPE_TAG = "error.type";

    /** Desenlace del envío, también emitido por {@link ResendEmailClient}. */
    static final String OUTCOME_TAG = "email.outcome";

    /** Valor con el que Micrometer marca la ausencia de error. */
    static final String NO_ERROR = "none";

    static final Set<String> OUTCOMES = Set.of("success", "failure", "skipped", "invalid",
            "misconfigured");

    /** Tope de avisos distintos, por si el valor inesperado llega en tromba. */
    static final int MAX_LOGGED_VALUES = 50;

    private static final Logger log = LoggerFactory.getLogger(EmailMetricCardinalityFilter.class);

    private final Set<String> loggedValues = ConcurrentHashMap.newKeySet();

    private final AtomicBoolean suppressionAnnounced = new AtomicBoolean();

    @Override
    public Meter.Id map(Meter.Id id) {
        if (!id.getName().startsWith(PREFIX)) {
            return id;
        }
        List<Tag> bounded = new ArrayList<>();
        boolean changed = false;
        for (Tag tag : id.getTags()) {
            String value = bound(tag);
            changed = changed || !value.equals(tag.getValue());
            bounded.add(Tag.of(tag.getKey(), value));
        }
        return changed ? id.replaceTags(bounded) : id;
    }

    private String bound(Tag tag) {
        return switch (tag.getKey()) {
            case ERROR_TAG -> NO_ERROR.equals(tag.getValue()) ? NO_ERROR : OTHER;
            case ERROR_TYPE_TAG -> allowed(EmailErrorType.tags(), tag);
            case OUTCOME_TAG -> allowed(OUTCOMES, tag);
            default -> tag.getValue();
        };
    }

    private String allowed(Set<String> declared, Tag tag) {
        if (declared.contains(tag.getValue())) {
            return tag.getValue();
        }
        warnOnce(tag);
        return OTHER;
    }

    private void warnOnce(Tag tag) {
        if (loggedValues.size() >= MAX_LOGGED_VALUES) {
            if (suppressionAnnounced.compareAndSet(false, true)) {
                log.warn("Se alcanzaron {} valores distintos no declarados en el filtro de"
                        + " cardinalidad de las métricas de correo; se deja de avisar de cada"
                        + " uno. Todos se siguen contando en {}=\"{}\".", MAX_LOGGED_VALUES,
                        ERROR_TYPE_TAG, OTHER);
            }
            return;
        }
        if (!loggedValues.add(tag.getKey() + "=" + tag.getValue())) {
            return;
        }
        log.warn("Valor no declarado en el filtro de cardinalidad de las métricas de correo: la"
                + " etiqueta \"{}\" llegó con \"{}\" y se publicará como \"{}\". Si el valor es"
                + " legítimo, decláralo (EmailErrorType para {}, OUTCOMES para {}); si no, deja"
                + " de emitirlo.", tag.getKey(), tag.getValue(), OTHER, ERROR_TYPE_TAG,
                OUTCOME_TAG);
    }
}
