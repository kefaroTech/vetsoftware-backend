package com.vetsoftware.app.aiproposal.infrastructure.observability;

import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics;
import com.vetsoftware.app.aiproposal.infrastructure.ai.AiErrorType;
import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricNames;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * El adaptador unico de {@link AiProposalMetrics}: publica los contadores y
 * <strong>ademas</strong> etiqueta el span de la operacion en curso.
 *
 * <p>
 * <strong>Las dos cosas en la misma llamada, y ese es el punto.</strong> Si la
 * metrica la emitiera el caso de uso y el atributo de span lo pusiera otro,
 * tarde o temprano una de las dos ramas se olvidaria de un camino y la traza
 * diria «degradado» donde el contador dice «succeeded». Con un unico emisor eso
 * no puede pasar: o salen las dos o no sale ninguna.
 *
 * <p>
 * &#9940; <strong>{@code highCardinalityKeyValue} y no
 * {@code lowCardinalityKeyValue}.</strong> En Micrometer lo de baja
 * cardinalidad viaja a la metrica <em>y</em> al span; lo de alta, solo al span.
 * El desenlace, la presentacion y la longitud del texto ya estan contados por
 * los contadores de aqui abajo, asi que declararlos de baja cardinalidad los
 * duplicaria como etiquetas del {@code Timer} de {@code aiproposal.generate}
 * —que ademas ya lleva {@code class}, {@code method} y {@code error}— y
 * multiplicaria su histograma por veinticuatro sin responder ninguna pregunta
 * nueva. Y {@code proposal.id} de baja cardinalidad seria una serie por
 * peticion, para siempre: es el fallo mas caro del catalogo y el mas frecuente.
 *
 * <p>
 * <strong>Lo que este adaptador no puede emitir aunque alguien se lo
 * pase.</strong> Su unico argumento es {@code ServedProposal}, cuyos campos son
 * enums, enteros y un {@code Long}. No hay ningun parametro de tipo
 * {@code String} por el que pueda entrar el texto del prospecto, la prosa del
 * modelo o el {@code public_token}. La prueba de fuga con valor senuelo lo
 * comprueba ejercitando el camino entero, incluidos los atributos del span, que
 * es la mitad que no cubria nadie.
 */
@Component
public class MicrometerAiProposalMetrics implements AiProposalMetrics {

    static final String OPERATION_TAG = "ai.operation";

    static final String OUTCOME_TAG = "ai.outcome";

    static final String PRESENTATION_TAG = "ai.presentation";

    /**
     * &#9940; <strong>La clase del fallo del modelo: {@code none},
     * {@code transient} o {@code systemic}, y nada mas.</strong>
     *
     * <p>
     * Se llama asi —y no {@code ai.error.type}— por dos motivos. Uno: sigue la
     * forma de las otras tres etiquetas de este medidor ({@code ai.operation},
     * {@code ai.outcome}, {@code ai.presentation}), asi que en Prometheus sale
     * {@code ai_failure_kind} y se lee en la misma linea que las demas. Dos: deja
     * libre {@code ai.failure.code}, que ya es el atributo del span con el codigo
     * exacto —trece valores— y que <strong>no</strong> es esto: aquello se consulta
     * de una traza en una, esto se agrega en una alerta.
     *
     * <p>
     * <strong>Se emite SIEMPRE, tambien en el camino feliz.</strong> Ver
     * {@link FailureKind#NONE}: Prometheus exige el mismo juego de claves en todas
     * las muestras de un medidor, y omitir la etiqueta reventaria el registro. Lo
     * comprueba {@code MicrometerAiProposalMetricsTest}, que monta un
     * {@code PrometheusMeterRegistry} justamente para eso.
     */
    static final String FAILURE_KIND_TAG = "ai.failure.kind";

    static final String RULE_TAG = "reason.rule";

    static final String VERDICT_TAG = "line.verdict";

    /** Longitud del texto del prospecto. Longitud, jamas el texto. */
    static final String INPUT_CHARS_ATTRIBUTE = "ai.input.chars";

    static final String PROPOSAL_ID_ATTRIBUTE = "proposal.id";

    static final String INVALID_LINES_ATTRIBUTE = "proposal.invalid.lines";

    static final String REJECTED_REASONS_ATTRIBUTE = "proposal.rejected.reasons";

    private final Meter.MeterProvider<Counter> generated;

    private final Meter.MeterProvider<Counter> reasonRejected;

    private final Meter.MeterProvider<Counter> invalidLines;

    private final ObservationRegistry observations;

    public MicrometerAiProposalMetrics(MeterRegistry registry, ObservationRegistry observations) {
        this.observations = observations;
        // Sin baseUnit, por el mismo motivo que document.delivery: el nombre no
        // termina en un sustantivo de unidad, asi que declarar una produciria
        // ..._generated_proposals_total, que nadie adivina al escribir la alerta.
        generated = Counter.builder(BusinessMetricNames.AI_PROPOSAL_GENERATED)
                .description("Propuestas servidas por el asistente, por operacion, desenlace y"
                        + " forma de presentarlas; ai_outcome=\"degraded_model_unavailable\""
                        + " sostenido significa que el producto lleva horas vendiendo sin IA")
                .withRegistry(registry);
        reasonRejected = Counter.builder(BusinessMetricNames.AI_PROPOSAL_REASON_REJECTED)
                .description("Motivos del modelo que el saneador sustituyo o trunco, por la regla"
                        + " que disparo; una regla que se dispara en la mayoria de las lineas es"
                        + " deriva del prompt, no del saneador")
                .withRegistry(registry);
        invalidLines = Counter.builder(BusinessMetricNames.AI_PROPOSAL_INVALID_LINES)
                .description("Codigos propuestos por el modelo que el motor determinista no pudo"
                        + " cotizar, por veredicto; es la medida de si el modelo alucina codigos"
                        + " de catalogo")
                .withRegistry(registry);
    }

    @Override
    public void proposalServed(ServedProposal served) {
        generated.withTags(OPERATION_TAG, served.operation().value(), OUTCOME_TAG,
                served.outcome().value(), PRESENTATION_TAG, lower(served.presentation()),
                FAILURE_KIND_TAG, claseDelFallo(served).value()).increment();
        served.rejectedReasons()
                .forEach(regla -> reasonRejected.withTag(RULE_TAG, lower(regla)).increment());
        served.rejectedLines().forEach(
                veredicto -> invalidLines.withTag(VERDICT_TAG, lower(veredicto)).increment());
        etiquetarSpan(served);
    }

    /**
     * El span de la operacion en curso, si lo hay. Es {@code null} cuando el caso
     * de uso se instancia a mano en una prueba unitaria o cuando la observacion
     * esta apagada, y eso no es motivo para perder el contador: por eso el contador
     * va primero y esto despues.
     *
     * <p>
     * <strong>No se fija el estado del span.</strong> Una degradacion sirvio una
     * propuesta utilizable con HTTP 200: marcarla {@code Error} haria que la traza
     * contradiga al log y al codigo de estado, y quemaria el presupuesto de error
     * de un servicio que no dejo de servir. Los turnos degradados se buscan por
     * {@code ai.outcome != "succeeded"}, no por el color.
     */
    private void etiquetarSpan(ServedProposal served) {
        Observation actual = observations.getCurrentObservation();
        if (actual == null) {
            return;
        }
        actual.highCardinalityKeyValue(OUTCOME_TAG, served.outcome().value())
                .highCardinalityKeyValue(PRESENTATION_TAG, lower(served.presentation()))
                .highCardinalityKeyValue(INPUT_CHARS_ATTRIBUTE,
                        Integer.toString(served.inputChars()))
                .highCardinalityKeyValue(INVALID_LINES_ATTRIBUTE,
                        Integer.toString(served.rejectedLines().size()))
                .highCardinalityKeyValue(REJECTED_REASONS_ATTRIBUTE,
                        Integer.toString(served.rejectedReasons().size()));
        if (served.proposalId() != null) {
            actual.highCardinalityKeyValue(PROPOSAL_ID_ATTRIBUTE,
                    Long.toString(served.proposalId()));
        }
    }

    /**
     * &#9940; <strong>La traduccion vive AQUI y no en el puerto, y eso es una regla
     * de arquitectura y no una preferencia.</strong> {@link AiErrorType} —quien
     * sabe si un codigo se cura solo— es {@code infrastructure.ai};
     * {@code AiProposalMetrics} es {@code application}. Que el puerto lo importara
     * invertiria la direccion de dependencias y lo tumbaria ArchUnit. Por eso
     * {@code ServedProposal} viaja con el {@code failureCode} en crudo y el mapa a
     * dos ramas se hace en este adaptador, que si es infraestructura.
     *
     * <p>
     * <strong>Lo desconocido cae en {@code systemic}</strong>, porque
     * {@code AiErrorType.deFailureCode} manda a {@code OTHER} lo que no reconoce y
     * {@code OTHER} es sistemico a proposito: un codigo sin rama es una rama que
     * falta, y fallar hacia el lado ruidoso es lo que hace que alguien lo arregle.
     */
    private static FailureKind claseDelFallo(ServedProposal served) {
        if (served.failureCode() == null) {
            return FailureKind.NONE;
        }
        return AiErrorType.deFailureCode(served.failureCode()).esSistemico()
                ? FailureKind.SYSTEMIC
                : FailureKind.TRANSIENT;
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
