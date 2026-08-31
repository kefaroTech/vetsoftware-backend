package com.vetsoftware.app.aiproposal.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationResult;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics.Operation;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics.Outcome;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics.ServedProposal;
import com.vetsoftware.app.aiproposal.domain.CartLine;
import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.LineSource;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.ReasonRejection;
import com.vetsoftware.app.aiproposal.domain.SanitizedReason;
import com.vetsoftware.app.aiproposal.domain.SellableItemKind;
import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricCardinalityFilter;
import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricNames;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El emisor de la telemetria del asistente, ejercitado contra un registro
 * <b>con el filtro de cardinalidad instalado</b>.
 *
 * <p>
 * Sin ese filtro la prueba diria que la metrica se publica cuando en produccion
 * la lista blanca la estaria denegando entera, que es el fallo silencioso que
 * este paquete existe para evitar: no hay excepcion, no hay log del emisor y el
 * panel muestra un hueco indistinguible de «no hubo prospectos».
 */
@DisplayName("MicrometerAiProposalMetrics — contadores y atributos de span de un turno servido")
class MicrometerAiProposalMetricsTest {

    private PrometheusMeterRegistry registry;

    private ObservationRegistry observations;

    private final List<Observation.Context> spans = new ArrayList<>();

    private MicrometerAiProposalMetrics metrics;

    @BeforeEach
    void montar() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        BusinessMetricCardinalityFilter filtro = new BusinessMetricCardinalityFilter();
        registry.config().meterFilter(filtro);
        // El contador de descartes lo publica el binder, no el filtro: sin esta
        // llamada la serie no existe y la comprobacion de «cero denegados» pasaria
        // sin comprobar nada.
        filtro.bindTo(registry);
        observations = ObservationRegistry.create();
        observations.observationConfig()
                .observationHandler(new ObservationHandler<Observation.Context>() {

                    @Override
                    public void onStop(Observation.Context context) {
                        spans.add(context);
                    }

                    @Override
                    public boolean supportsContext(Observation.Context context) {
                        return true;
                    }
                });
        metrics = new MicrometerAiProposalMetrics(registry, observations);
    }

    private static CartResult carritoCon(LineVerdict... veredictos) {
        List<CartLine> lineas = new ArrayList<>();
        int orden = 0;
        for (LineVerdict veredicto : veredictos) {
            lineas.add(new CartLine("COD" + orden, "Modulo", "corto", SellableItemKind.MODULE,
                    LineSource.MODEL, veredicto, 1, new BigDecimal("1000.00"),
                    new BigDecimal("19.00"), 0, "COP", "un motivo cualquiera", orden++));
        }
        return new CartResult(lineas, "COP");
    }

    @Nested
    @DisplayName("Contadores")
    class Contadores {

        @Test
        @DisplayName("una propuesta servida cuenta una vez, con operacion, desenlace y presentacion")
        void una_propuesta_servida_cuenta_una_vez() {
            metrics.proposalServed(new ServedProposal(Operation.PROPOSE, Outcome.SUCCEEDED,
                    ProposalPresentation.PROPOSAL, null, List.of(), List.of(), 42, 7L));

            assertThat(registry.get(BusinessMetricNames.AI_PROPOSAL_GENERATED)
                    .tag("ai.operation", "propose").tag("ai.outcome", "succeeded")
                    .tag("ai.presentation", "proposal").counter().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("cada motivo rechazado cuenta en su regla, que es vocabulario cerrado")
        void cada_motivo_rechazado_cuenta_en_su_regla() {
            metrics.proposalServed(new ServedProposal(Operation.REFINE, Outcome.SUCCEEDED,
                    ProposalPresentation.PROPOSAL, null, List.of(ReasonRejection.R3_CIFRA,
                            ReasonRejection.R3_CIFRA, ReasonRejection.R7_CODIGO),
                    List.of(), 30, 7L));

            assertThat(registry.get(BusinessMetricNames.AI_PROPOSAL_REASON_REJECTED)
                    .tag("reason.rule", "r3_cifra").counter().count()).isEqualTo(2);
            assertThat(registry.get(BusinessMetricNames.AI_PROPOSAL_REASON_REJECTED)
                    .tag("reason.rule", "r7_codigo").counter().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("cada codigo que el modelo invento cuenta en su veredicto")
        void cada_codigo_invalido_cuenta_en_su_veredicto() {
            metrics.proposalServed(new ServedProposal(Operation.PROPOSE, Outcome.SUCCEEDED,
                    ProposalPresentation.PROPOSAL, null, List.of(),
                    List.of(LineVerdict.UNKNOWN_CODE, LineVerdict.NOT_SELF_SERVICE), 30, 7L));

            assertThat(registry.get(BusinessMetricNames.AI_PROPOSAL_INVALID_LINES)
                    .tag("line.verdict", "unknown_code").counter().count()).isEqualTo(1);
            assertThat(registry.get(BusinessMetricNames.AI_PROPOSAL_INVALID_LINES)
                    .tag("line.verdict", "not_self_service").counter().count()).isEqualTo(1);
        }

        /**
         * &#9940; <b>Las dos etiquetas dicen lo mismo, y esa es la prueba.</b> Hasta el
         * #692 este camino salia con {@code ai.presentation="deterministic"}, es decir
         * mezclado con las degradaciones del modelo -que SI sirven lineas- en cualquier
         * panel filtrado por presentacion. Una etiqueta que desmiente a la otra en la
         * misma muestra es peor que no tenerla.
         */
        @Test
        @DisplayName("el camino sin tarifa publicada cuenta con las dos etiquetas en no_catalog")
        void el_camino_sin_catalogo_cuenta() {
            metrics.proposalServed(ServedProposal.sinCatalogo(Operation.PROPOSE, 55));

            assertThat(registry.get(BusinessMetricNames.AI_PROPOSAL_GENERATED)
                    .tag("ai.outcome", "no_catalog").tag("ai.presentation", "no_catalog").counter()
                    .count()).isEqualTo(1);
        }

        /**
         * &#9940; <b>La tarifa publicada y vacia NO es el mismo desenlace.</b> La
         * accion es la contraria -alli hay que publicar la tarifa, aqui ya esta
         * publicada-, y con los dos colapsados la alerta mandaba a comprobar algo que
         * estaba bien.
         */
        @Test
        @DisplayName("la tarifa publicada pero sin articulos cuenta como empty_catalog, no como"
                + " no_catalog")
        void el_catalogo_vacio_no_se_confunde_con_la_falta_de_tarifa() {
            metrics.proposalServed(ServedProposal.catalogoVacio(Operation.PROPOSE, 55));

            assertThat(registry.get(BusinessMetricNames.AI_PROPOSAL_GENERATED)
                    .tag("ai.outcome", "empty_catalog").counter().count()).isEqualTo(1);
            assertThat(registry.find(BusinessMetricNames.AI_PROPOSAL_GENERATED)
                    .tag("ai.outcome", "no_catalog").counter()).isNull();
        }
    }

    /**
     * &#9940; <b>La particion del fallo del modelo en las dos poblaciones que se
     * atienden al reves.</b> Un tiempo agotado se cura solo; unas credenciales mal
     * puestas fallan el 100 % hasta que alguien entre a cambiar configuracion. La
     * distincion ya existia en el nivel del log y <b>no</b> en el contador, asi que
     * quien miraba la serie veia un solo {@code model_failed}.
     */
    @Nested
    @DisplayName("La clase del fallo, en dos ramas")
    class ClaseDelFallo {

        @Test
        @DisplayName("un fallo que se cura solo sale como transient")
        void un_fallo_transitorio_sale_como_transient() {
            metrics.proposalServed(new ServedProposal(Operation.PROPOSE, Outcome.MODEL_FAILED,
                    ProposalPresentation.DETERMINISTIC, "MODEL_RATE_LIMITED", List.of(), List.of(),
                    10, 7L));

            assertThat(registry.get(BusinessMetricNames.AI_PROPOSAL_GENERATED)
                    .tag("ai.failure.kind", "transient").counter().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("un fallo que no se cura solo sale como systemic")
        void un_fallo_sistemico_sale_como_systemic() {
            metrics.proposalServed(new ServedProposal(Operation.PROPOSE, Outcome.MODEL_FAILED,
                    ProposalPresentation.DETERMINISTIC, "MODEL_FORBIDDEN", List.of(), List.of(), 10,
                    7L));

            assertThat(registry.get(BusinessMetricNames.AI_PROPOSAL_GENERATED)
                    .tag("ai.failure.kind", "systemic").counter().count()).isEqualTo(1);
        }

        /**
         * &#9940; Un codigo sin rama cae en {@code AiErrorType.OTHER}, que es sistemico
         * <b>a proposito</b>: fallar hacia el lado ruidoso es lo que hace que alguien
         * añada la rama. Lo contrario convierte cada codigo nuevo del proveedor en
         * ruido de fondo que nadie mira.
         */
        @Test
        @DisplayName("un codigo desconocido cae en systemic, que es el lado ruidoso")
        void un_codigo_desconocido_cae_en_systemic() {
            metrics.proposalServed(new ServedProposal(Operation.PROPOSE, Outcome.MODEL_FAILED,
                    ProposalPresentation.DETERMINISTIC, "ALGO_QUE_NADIE_DECLARO", List.of(),
                    List.of(), 10, 7L));

            assertThat(registry.get(BusinessMetricNames.AI_PROPOSAL_GENERATED)
                    .tag("ai.failure.kind", "systemic").counter().count()).isEqualTo(1);
        }

        /**
         * &#9940; <b>La etiqueta se emite SIEMPRE, y esta prueba es la unica red que
         * hay.</b> {@code PrometheusMeterRegistry} exige el mismo juego de claves en
         * todas las muestras de un medidor; omitirla en el camino feliz reventaria el
         * registro. Que esta clase monte un registro de Prometheus <b>y no un
         * {@code SimpleMeterRegistry}</b> es lo que hace que eso salte aqui y no en el
         * primer arranque.
         */
        @Test
        @DisplayName("el camino feliz tambien lleva la etiqueta, con valor none")
        void el_camino_feliz_tambien_lleva_la_etiqueta() {
            metrics.proposalServed(new ServedProposal(Operation.PROPOSE, Outcome.SUCCEEDED,
                    ProposalPresentation.PROPOSAL, null, List.of(), List.of(), 10, 7L));

            assertThat(registry.get(BusinessMetricNames.AI_PROPOSAL_GENERATED).counter().getId()
                    .getTag("ai.failure.kind")).isEqualTo("none");
        }
    }

    @Nested
    @DisplayName("Atributos de span")
    class AtributosDeSpan {

        @Test
        @DisplayName("el span lleva el desenlace, la longitud del texto y el id numerico, nunca el token")
        void el_span_lleva_lo_que_el_contador_no_puede() {
            Observation.createNotStarted("aiproposal.generate", observations)
                    .observe(() -> metrics.proposalServed(new ServedProposal(Operation.PROPOSE,
                            Outcome.MODEL_FAILED, ProposalPresentation.DETERMINISTIC,
                            "MODEL_TIMEOUT", List.of(ReasonRejection.R3_CIFRA),
                            List.of(LineVerdict.UNKNOWN_CODE), 123, 7L)));

            assertThat(atributos()).contains(KeyValue.of("ai.outcome", "model_failed"),
                    KeyValue.of("ai.presentation", "deterministic"),
                    KeyValue.of("ai.input.chars", "123"),
                    KeyValue.of("proposal.invalid.lines", "1"),
                    KeyValue.of("proposal.rejected.reasons", "1"), KeyValue.of("proposal.id", "7"));
        }

        @Test
        @DisplayName("una degradacion NO marca el span como error: se sirvio una propuesta valida con 200")
        void una_degradacion_no_pinta_el_span_de_rojo() {
            Observation.createNotStarted("aiproposal.generate", observations)
                    .observe(() -> metrics.proposalServed(new ServedProposal(Operation.PROPOSE,
                            Outcome.DEGRADED_SPEND_CAP, ProposalPresentation.DETERMINISTIC, null,
                            List.of(), List.of(), 20, 7L)));

            assertThat(spans).singleElement()
                    .satisfies(span -> assertThat(span.getError()).isNull());
        }

        @Test
        @DisplayName("sin propuesta persistida no se inventa un proposal.id")
        void sin_propuesta_persistida_no_hay_id() {
            Observation.createNotStarted("aiproposal.generate", observations).observe(() -> metrics
                    .proposalServed(ServedProposal.sinCatalogo(Operation.PROPOSE, 20)));

            assertThat(atributos()).noneMatch(kv -> kv.getKey().equals("proposal.id"));
        }

        @Test
        @DisplayName("sin observacion en curso el contador sale igual: la metrica no depende del span")
        void sin_span_el_contador_sale_igual() {
            metrics.proposalServed(ServedProposal.sinCatalogo(Operation.REFINE, 20));

            assertThat(registry.get(BusinessMetricNames.AI_PROPOSAL_GENERATED)
                    .tag("ai.operation", "refine").counter().count()).isEqualTo(1);
            assertThat(spans).isEmpty();
        }

        private List<KeyValue> atributos() {
            return spans.stream().flatMap(span -> span.getAllKeyValues().stream()).toList();
        }
    }

    @Nested
    @DisplayName("La derivacion del turno")
    class Derivacion {

        @Test
        @DisplayName("solo cuenta como alucinacion lo que propuso el modelo, no lo que arrastro el cierre")
        void solo_cuenta_lo_que_propuso_el_modelo() {
            CartResult carrito = new CartResult(
                    List.of(linea("MALO", LineSource.MODEL, LineVerdict.UNKNOWN_CODE, 0),
                            linea("ARRASTRADO", LineSource.DEPENDENCY_CLOSURE,
                                    LineVerdict.NOT_SELLABLE, 1),
                            linea("BUENO", LineSource.MODEL, LineVerdict.ACCEPTED, 2)),
                    "COP");
            ProposalDraft draft = new ProposalDraft(true, false, List.of("MALO", "BUENO"),
                    List.of(),
                    Map.of("MALO",
                            SanitizedReason.sustituido("fallback", ReasonRejection.R4_DINERO),
                            "BUENO", SanitizedReason.intacto("un motivo largo y limpio")),
                    null, 0);

            ServedProposal medida = ServedProposal.de(
                    Operation.PROPOSE, new ProposalGenerationResult(GenerationOutcome.SUCCEEDED,
                            draft, null, null, null),
                    ProposalPresentation.PROPOSAL, carrito, 44, 7L);

            assertThat(medida.rejectedLines()).containsExactly(LineVerdict.UNKNOWN_CODE);
            assertThat(medida.rejectedReasons()).containsExactly(ReasonRejection.R4_DINERO);
            assertThat(medida.outcome()).isEqualTo(Outcome.SUCCEEDED);
        }

        @Test
        @DisplayName("una longitud negativa no se puede construir: seria un contador roto sin ruido")
        void una_longitud_negativa_no_se_construye() {
            assertThatThrownBy(() -> new ServedProposal(Operation.PROPOSE, Outcome.SUCCEEDED,
                    ProposalPresentation.PROPOSAL, null, null, null, -1, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inputChars");
        }

        @Test
        @DisplayName("las listas nulas son listas vacias, no un NullPointerException en el emisor")
        void las_listas_nulas_son_vacias() {
            ServedProposal medida = new ServedProposal(Operation.REFINE, Outcome.SUCCEEDED,
                    ProposalPresentation.PROPOSAL, null, null, null, 0, null);

            assertThat(medida.rejectedReasons()).isEmpty();
            assertThat(medida.rejectedLines()).isEmpty();
        }

        private static CartLine linea(String code, LineSource source, LineVerdict verdict,
                int orden) {
            return new CartLine(code, "Modulo", "corto", SellableItemKind.MODULE, source, verdict,
                    1, new BigDecimal("1000.00"), new BigDecimal("19.00"), 0, "COP",
                    "un motivo cualquiera", orden);
        }
    }

    @Test
    @DisplayName("ningun medidor del asistente queda denegado por la lista blanca")
    void ningun_medidor_queda_denegado() {
        for (Outcome outcome : Outcome.values()) {
            metrics.proposalServed(new ServedProposal(Operation.PROPOSE, outcome,
                    ProposalPresentation.DETERMINISTIC, null, List.of(ReasonRejection.values()),
                    List.of(LineVerdict.values()), 10, 7L));
        }

        assertThat(registry.find(BusinessMetricNames.AI_PROPOSAL_GENERATED).counters())
                .hasSize(Outcome.values().length);
        assertThat(registry.find(BusinessMetricNames.AI_PROPOSAL_REASON_REJECTED).counters())
                .hasSize(ReasonRejection.values().length);
        assertThat(registry.find(BusinessMetricNames.AI_PROPOSAL_INVALID_LINES).counters())
                .hasSize(LineVerdict.values().length);
        assertThat(registry.get(BusinessMetricCardinalityFilter.DENIED).functionCounters())
                .allSatisfy(contador -> assertThat(contador.count()).isZero());
    }

    @Test
    @DisplayName("un carrito solo con lineas aceptadas no publica ninguna serie de alucinacion")
    void sin_alucinaciones_no_hay_serie() {
        metrics.proposalServed(new ServedProposal(Operation.PROPOSE, Outcome.SUCCEEDED,
                ProposalPresentation.PROPOSAL, null, List.of(), List.of(), 10, 7L));

        assertThat(registry.find(BusinessMetricNames.AI_PROPOSAL_INVALID_LINES).counters())
                .isEmpty();
        assertThat(carritoCon(LineVerdict.ACCEPTED).descartadas()).isZero();
    }
}
