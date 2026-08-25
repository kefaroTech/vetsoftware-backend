package com.vetsoftware.app.infrastructure.observability.business;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lista blanca para las dimensiones de negocio. Cualquier clave o valor nuevo
 * debe revisarse deliberadamente antes de aumentar la cantidad de series en
 * Prometheus.
 *
 * <p>
 * Un descarte deja rastro por dos vías, porque responden preguntas distintas:
 * el contador {@value #DENIED} permite alertar sobre la tasa y saber qué
 * medidor quedó ciego; el registro dice qué etiqueta lo provocó y qué hay que
 * hacer. Sin rastro, un valor nuevo de un enum ciega un panel para siempre y el
 * hueco resultante es indistinguible de la ausencia de actividad.
 *
 * <p>
 * El contador vive fuera de {@link BusinessMetricNames#PREFIX} a propósito. Su
 * etiqueta {@code metric} no está en la lista blanca, así que dentro del
 * prefijo este mismo filtro lo denegaría y el arreglo se comería a sí mismo: el
 * único rastro del descarte sería descartado.
 *
 * <p>
 * El contador tampoco se registra dentro de {@code accept}. El registro invoca
 * los filtros dentro de su propio bloque sincronizado, en mitad del alta de
 * otro medidor, y un {@code MeterFilter} que dependiera del
 * {@code MeterRegistry} sería además un ciclo de beans, porque Spring aplica
 * los filtros mientras construye el registro. Por eso el filtro solo toca
 * acumuladores en memoria y los publica como {@link MeterBinder}, que Spring
 * aplica después de los filtros y con el registro ya creado.
 *
 * <p>
 * Las series se pre-registran a cero para todo el catálogo de
 * {@link BusinessMetricNames}, más el cubo {@value #OTHER}. Son unas quince
 * series constantes, y son las que permiten que una alerta
 * {@code increase(...) > 0} funcione desde el primer scrape en vez de depender
 * de que la serie aparezca por primera vez justo durante el incidente.
 *
 * <p>
 * El nivel es {@code ERROR} y no {@code WARN} a propósito: el incremento
 * descartado no se reintenta, no se encola y no lo recupera nadie; solo una
 * persona cambiando esta lista y desplegando lo arregla, que es la definición
 * de fallo terminal. Se emite una sola vez por {@link Meter.Id} y el conjunto
 * de vistos está acotado, porque los identificadores denegados son justamente
 * los de cardinalidad no acotada.
 */
public final class BusinessMetricCardinalityFilter implements MeterFilter, MeterBinder {

    /** Contador de descartes. Fuera del prefijo de negocio: ver el javadoc. */
    public static final String DENIED = "vetsoftware.observability.metrics.denied";

    /** Cubo de reserva para un medidor de negocio ausente del catálogo. */
    static final String OTHER = "other";

    /** Tope de identificadores registrados, por si el descarte es una tormenta. */
    static final int MAX_LOGGED_IDS = 100;

    private static final Logger log = LoggerFactory
            .getLogger(BusinessMetricCardinalityFilter.class);

    private static final Set<String> COMMON_TAGS = Set.of("application", "environment", "instance",
            "region", "service");

    private static final Map<String, Set<String>> ALLOWED_VALUES = Map.ofEntries(
            // "failed" lo emite vetsoftware.business.document.delivery (issue #85). El
            // camino
            // feliz de esa entrega usa "success", ya declarado.
            // Los doce ultimos los emite el alta de superadministradores de plataforma
            // (vetsoftware.business.system.user.*, contrato en
            // docs/TELEMETRIA_ALTA_SUPERADMIN.md 4.3). Olvidar uno solo no degrada la
            // serie: deniega el medidor ENTERO, y el hueco del panel es
            // indistinguible de "no hubo actividad". "rejected", "duplicate_ignored",
            // "success" y "failed" ya estaban y se reutilizan a proposito, en vez de
            // abrir un vocabulario paralelo para el mismo concepto.
            Map.entry("result",
                    Set.of("completed", "rejected", "cancelled", "error", "validated",
                            "contingency", "pending", "success", "failed", "insufficient_stock",
                            "duplicate_ignored", "validation_error", "difference", "form_closed",
                            "approved", "token_invalid", "token_expired", "token_consumed",
                            "code_mismatch", "attempts_exhausted", "sent", "skipped", "accepted",
                            "expired", "email_already_provisioned")),
            Map.entry("channel", Set.of("pos", "open_account", "staff", "public")),
            Map.entry("document.type",
                    Set.of("fe_venta", "doc_equiv_pos", "nota_credito", "nota_debito", "unknown")),
            Map.entry("origin", Set.of("initial", "retry", "webhook", "reconciliation")),
            Map.entry("status",
                    Set.of("pending", "contingency", "requested", "confirmed", "arrived",
                            "in_progress", "completed", "no_show", "cancelled")),
            Map.entry("age",
                    Set.of("lt_15m", "from_15m_to_1h", "gt_1h", "expired", "from_0_to_7d",
                            "from_8_to_30d")),
            Map.entry("movement.type",
                    Set.of("purchase", "sale", "clinical_use", "adjustment_in", "adjustment_out",
                            "transfer_in", "transfer_out", "void_in", "void_out")),
            Map.entry("event", Set.of("opened", "closed")),
            Map.entry("direction", Set.of("shortage", "surplus", "balanced")));

    /**
     * Acumuladores de descarte por nombre de medidor. El conjunto de claves es fijo
     * desde la construcción, así que {@code accept} nunca hace crecer este mapa. La
     * referencia fuerte es necesaria: {@link FunctionCounter} solo guarda una
     * referencia débil a su estado.
     */
    private final Map<String, LongAdder> denials = catalogCounters();

    private final Set<Meter.Id> loggedIds = ConcurrentHashMap.newKeySet();

    private final AtomicBoolean suppressionAnnounced = new AtomicBoolean();

    @Override
    public MeterFilterReply accept(Meter.Id id) {
        if (!id.getName().startsWith(BusinessMetricNames.PREFIX)) {
            return MeterFilterReply.NEUTRAL;
        }
        for (Tag tag : id.getTags()) {
            if (COMMON_TAGS.contains(tag.getKey())) {
                continue;
            }
            Set<String> values = ALLOWED_VALUES.get(tag.getKey());
            if (values == null || !values.contains(tag.getValue())) {
                recordDenial(id, tag, values == null);
                return MeterFilterReply.DENY;
            }
        }
        return MeterFilterReply.NEUTRAL;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        denials.forEach((metric, count) -> FunctionCounter.builder(DENIED, count, LongAdder::sum)
                .description("Medidores de negocio descartados por la lista blanca de cardinalidad;"
                        + " cualquier valor mayor que cero es un panel ciego")
                .tag("metric", metric).register(registry));
    }

    private void recordDenial(Meter.Id id, Tag tag, boolean undeclaredKey) {
        denials.getOrDefault(id.getName(), denials.get(OTHER)).increment();
        if (!shouldLog(id)) {
            return;
        }
        if (undeclaredKey) {
            log.error("Etiqueta no declarada en la lista blanca de cardinalidad: la métrica"
                    + " de negocio {} no se publicará mientras siga emitiendo la"
                    + " etiqueta \"{}\". Si es un identificador, la corrección es dejar"
                    + " de emitirlo como etiqueta de métrica -nunca añadirlo a la lista"
                    + " blanca-: pertenece a un atributo de span o a un campo de log."
                    + " Medidor completo: {}", id.getName(), tag.getKey(), id);
            return;
        }
        log.error("Valor no declarado en la lista blanca de cardinalidad: la métrica de negocio"
                + " {} no se publicará mientras el valor \"{}\" de la etiqueta \"{}\" no"
                + " se añada a ALLOWED_VALUES en BusinessMetricCardinalityFilter. El"
                + " panel mostrará un hueco indistinguible de la ausencia de actividad."
                + " Medidor completo: {}", id.getName(), tag.getValue(), tag.getKey(), id);
    }

    private boolean shouldLog(Meter.Id id) {
        if (loggedIds.size() >= MAX_LOGGED_IDS) {
            if (suppressionAnnounced.compareAndSet(false, true)) {
                log.error(
                        "Se alcanzaron {} identificadores distintos denegados por la lista"
                                + " blanca de cardinalidad; se deja de registrar cada uno para"
                                + " no inundar el log. El contador {} los sigue contando todos.",
                        MAX_LOGGED_IDS, DENIED);
            }
            return false;
        }
        return loggedIds.add(id);
    }

    /**
     * Un acumulador por nombre publicado en {@link BusinessMetricNames}. Se lee por
     * reflexión en vez de copiarse a mano porque una segunda lista escrita a mano
     * es exactamente el defecto que este contador existe para detectar.
     */
    private static Map<String, LongAdder> catalogCounters() {
        Map<String, LongAdder> counters = new HashMap<>();
        for (Field field : BusinessMetricNames.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) {
                continue;
            }
            try {
                Object value = field.get(null);
                if (value instanceof String name && name.startsWith(BusinessMetricNames.PREFIX)
                        && !name.equals(BusinessMetricNames.PREFIX)) {
                    counters.put(name, new LongAdder());
                }
            } catch (IllegalAccessException exception) {
                // Degradar al cubo de reserva es preferible a impedir el arranque por no poder
                // leer una constante pública del mismo paquete.
                log.warn(
                        "No se pudo leer {} del catálogo de métricas de negocio; sus descartes"
                                + " se contarán en metric=\"{}\"",
                        field.getName(), OTHER, exception);
            }
        }
        counters.put(OTHER, new LongAdder());
        return Map.copyOf(counters);
    }
}
