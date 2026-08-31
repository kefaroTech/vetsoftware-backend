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
            Map.entry("direction", Set.of("shortage", "surplus", "balanced")),
            // ── Dinero de suscripciones (#606) ──────────────────────────────────
            //
            // Seis claves nuevas, todas derivadas de un enum de dominio con
            // lower(...). La paridad enum ↔ lista blanca la sostiene
            // BusinessMetricEnumAllowlistParityTest: anadir una constante a
            // cualquiera de esos enums sin tocar este Map rompe el build, que es
            // preferible a descubrirlo cuando el panel del cierre de mes lleve
            // semanas con un hueco que parece calma.
            //
            // NO HAY NI UNA CLAVE DE IDENTIFICADOR, y no la puede haber: el
            // subscriptionId, el companyId y el numero de factura pertenecen a un
            // atributo de span o a un campo de log, nunca a una etiqueta de
            // metrica. Si alguna vez aparece una aqui, la correccion es quitarla
            // del emisor -no anadirla a esta lista-, y el ERROR que emite
            // recordDenial lo dice con esas palabras.
            //
            // Cardinalidad que anaden, contada: charge.type 5 x result 6 = 30 en
            // el peor caso teorico de subscription.charges, pero los desenlaces
            // realmente emitidos por clase son 2, asi que el techo practico es 10.
            // Las siete metricas juntas no llegan a 60 series aun emitiendo todas
            // las combinaciones.
            Map.entry("charge.type",
                    Set.of("recurring", "proration", "one_time", "credit", "discount", "overage")),
            // El signo del importe, y no es decoracion. DistributionSummary de
            // Micrometer DESCARTA EN SILENCIO los valores negativos, y en este
            // dominio los negativos son operaciones normales: un credito, un
            // descuento y una proracion de reduccion restan. Sin este tag habria
            // que registrar el valor absoluto y el histograma diria que se
            // devengaron 500.000 pesos cuando en realidad se devolvieron. Con el,
            // el neto es una resta de dos series y cada lado significa algo por si
            // solo. Un importe cero se cuenta como «debit»: no suma en ninguno de
            // los dos lados, y una tercera serie que solo contiene ceros es coste
            // sin informacion.
            Map.entry("charge.sign", Set.of("debit", "credit")),
            Map.entry("issue.status",
                    Set.of("draft", "awaiting_external", "external_registered", "voided")),
            Map.entry("payment.method", Set.of("transfer", "card", "pse", "cash", "other")),
            Map.entry("source.kind",
                    Set.of("payment", "credit_note", "withholding", "customer_credit", "rounding",
                            "write_off")),
            Map.entry("to.status",
                    Set.of("trialing", "active", "past_due", "read_only", "cancelled", "expired")),
            // Disparador del recalculo de entitlements. Vocabulario cerrado
            // definido en SubscriptionEntitlementMetrics.Trigger, no un enum de
            // dominio: describe POR QUE se recalculo, y eso es una decision de
            // este slice de telemetria. Dos valores porque son dos poblaciones
            // con dueno distinto -un pico a las 3 de la manana es el barrido; un
            // pico al mediodia es un incidente que estan viendo clientes- y
            // meterlas en la misma serie esconde la segunda detras de la primera.
            Map.entry("trigger.reason", Set.of("subscription_changed", "scheduled_sweep")),
            // ── Asistente comercial con IA (aiproposal) ─────────────────────────
            //
            // Cinco vocabularios cerrados y NI UNO abierto, aunque este es el
            // unico bloque del sistema alimentado por texto que escribe un
            // anonimo de internet. Lo que ese anonimo escribe -y lo que el
            // modelo devuelve en prosa- no llega hasta aqui: llega su longitud,
            // el enum de la regla que lo rechazo y el enum del veredicto. La
            // correccion, si algun dia aparece un codigo de catalogo o un
            // public_token como valor, es quitarlo del emisor y NUNCA anadirlo
            // a esta lista.
            //
            // La paridad enum <-> lista blanca la sostiene
            // BusinessMetricEnumAllowlistParityTest: una constante nueva en
            // GenerationOutcome, ProposalPresentation, ReasonRejection,
            // LineVerdict o AiProposalRetentionMetrics.Paso sin tocar este Map
            // rompe el build. Sin esa red, el valor nuevo deniega el MEDIDOR
            // ENTERO -no esa serie suelta- y el hueco del panel es
            // indistinguible de «no hubo prospectos».
            //
            // Cardinalidad, contada: generated = 2 x 7 x 5 x 3 = 210 en el peor
            // caso teorico, ~18 reales, y el peor caso teorico no lo alcanza nadie
            // porque las cuatro etiquetas estan fuertemente correlacionadas:
            // presentation esta determinado por outcome salvo cuando el modelo
            // respondio, y failure.kind solo deja de valer "none" cuando
            // outcome=model_failed (2 combinaciones mas, no 3 x lo anterior).
            // reason.rejected = 9; invalid.lines = 5; retention.rows = 6; spend y
            // spend.today no llevan etiqueta. Total <= 232 series, ~39 reales.
            //
            // El quinto valor de ai.presentation es no_catalog, y no anade
            // combinaciones reales: solo lo emiten ServedProposal.sinCatalogo y
            // .catalogoVacio, que fijan a la vez el outcome. Antes ese camino se
            // etiquetaba deterministic y se mezclaba con las degradaciones del
            // modelo, que SI sirven lineas.
            //
            // El septimo valor de ai.outcome es empty_catalog, y existe porque
            // no_catalog colapsaba dos estados con remedios opuestos: alli hay que
            // publicar la tarifa, aqui la tarifa ya esta publicada.
            Map.entry("ai.operation", Set.of("propose", "refine")),
            Map.entry("ai.outcome", Set.of("succeeded", "degraded_spend_cap", "degraded_no_hints",
                    "degraded_model_unavailable", "model_failed", "no_catalog", "empty_catalog")),
            Map.entry("ai.presentation",
                    Set.of("proposal", "not_understood", "out_of_domain", "deterministic",
                            "no_catalog")),
            // La clase del fallo del modelo, en dos ramas utiles mas el camino
            // feliz. NO son los trece valores de AiErrorType: quien recibe la
            // alerta decide entre "espera, se cura solo" y "entra a mirar
            // configuracion", y el codigo exacto sigue estando en el span del
            // intento (error.type), que se consulta de una traza en una. "none" va
            // porque Prometheus exige el mismo juego de claves en todas las
            // muestras del medidor -ver AiProposalMetrics.FailureKind-.
            Map.entry("ai.failure.kind", Set.of("none", "transient", "systemic")),
            Map.entry("reason.rule",
                    Set.of("r1_corto", "r2_largo", "r3_cifra", "r4_dinero", "r5_marcado",
                            "r6_enlace", "r7_codigo", "r8_contacto", "r9_repetido")),
            // «accepted» esta declarado aunque hoy no se emita -el contador solo
            // cuenta rechazos-. Cuesta cero mientras nadie lo emita y evita que
            // el dia que alguien quiera contar tambien lo aceptado se encuentre
            // con el medidor entero denegado en silencio.
            Map.entry("line.verdict",
                    Set.of("accepted", "unknown_code", "not_sellable", "not_self_service",
                            "duplicate")),
            Map.entry("retention.step",
                    Set.of("anonymize_proposals", "redact_turns", "redact_line_reasons",
                            "purge_lines", "purge_turns", "purge_acceptances", "purge_proposals")));

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
