package com.vetsoftware.app.aiproposal.infrastructure.retention;

import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricNames;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Lo que el barrido de retencion publica.
 *
 * <p>
 * <strong>Un barrido sin metrica es un barrido que nadie sabe si
 * corrio.</strong> El resultado del job -{@code no_work}, {@code success},
 * {@code partial_failure}- ya lo emite {@code ScheduledJobTelemetry} como
 * etiqueta de la observacion, y el heartbeat dice cuando termino bien por
 * ultima vez. Lo que ninguno de los dos dice es <em>cuanto</em> movio, y esa es
 * la pregunta que distingue "no habia nada que anonimizar" de "el paso de
 * motivos no toco una sola fila porque su subconsulta esta mal": los dos casos
 * sellan el heartbeat, los dos dan verde, y solo el contador por paso los
 * separa.
 *
 * <p>
 * &#9940; <strong>Y por eso el contador es por PASO y no un total.</strong> Un
 * unico numero de "filas tratadas" deja invisible justo el defecto que motivo
 * todo esto: la cabecera anonimizada y los motivos del prospecto intactos en la
 * tabla de al lado. Con {@code retention.step} separado, una anonimizacion sana
 * mueve las tres series a la vez y una rota se ve como dos series subiendo y
 * una plana.
 *
 * <p>
 * Vocabulario cerrado -{@link Paso}, seis valores- porque la etiqueta de una
 * metrica no puede admitir texto libre: es la forma clasica de reventar la
 * cardinalidad de Prometheus.
 *
 * <p>
 * <strong>Y desde la fase de telemetria de la rodaja, ese vocabulario esta
 * vigilado.</strong> Los dos medidores viven bajo {@code vetsoftware.business.}
 * y sus nombres en {@code BusinessMetricNames}, asi que
 * {@code BusinessMetricCardinalityFilter} comprueba la etiqueta
 * {@code retention.step} y {@code BusinessMetricEnumAllowlistParityTest} rompe
 * el build si se anade un paso sin declararlo. Antes estaban fuera del prefijo:
 * el vocabulario era cerrado por disciplina y no por mecanismo, y sus descartes
 * no tenian contador pre-registrado a cero.
 *
 * <p>
 * <strong>El gauge de lotes agotados no es un lujo.</strong> Publica cuantos
 * pasos terminaron la pasada con su cupo consumido, es decir cuantos dejaron
 * trabajo sin hacer. Un barrido que agota el cupo todas las noches esta
 * perdiendo terreno contra el ritmo de entrada y hay que subirle
 * {@code max-batches-per-run}; sin este numero eso solo se descubre el dia que
 * alguien mira la tabla y encuentra correos de hace ocho meses.
 */
@Component
public class AiProposalRetentionMetrics {

    static final String ROWS_METRIC = BusinessMetricNames.AI_PROPOSAL_RETENTION_ROWS;

    static final String EXHAUSTED_METRIC = BusinessMetricNames.AI_PROPOSAL_RETENTION_EXHAUSTED;

    static final String STEP_TAG = "retention.step";

    /**
     * Los seis pasos, en el orden en que corren. Es el vocabulario de la etiqueta.
     */
    public enum Paso {

        ANONIMIZAR_PROPUESTAS("anonymize_proposals"), REDACTAR_TURNOS(
                "redact_turns"), REDACTAR_MOTIVOS("redact_line_reasons"), PURGAR_LINEAS(
                        "purge_lines"), PURGAR_TURNOS(
                                "purge_turns"), PURGAR_PROPUESTAS("purge_proposals");

        private final String etiqueta;

        Paso(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String etiqueta() {
            return etiqueta;
        }
    }

    private final Map<Paso, Counter> filas = new EnumMap<>(Paso.class);

    private final AtomicLong pasosConCupoAgotado = new AtomicLong();

    public AiProposalRetentionMetrics(MeterRegistry registry) {
        for (Paso paso : Paso.values()) {
            filas.put(paso,
                    Counter.builder(ROWS_METRIC)
                            .description("Filas movidas por la politica de retencion de propuestas")
                            .tag(STEP_TAG, paso.etiqueta()).register(registry));
        }
        Gauge.builder(EXHAUSTED_METRIC, pasosConCupoAgotado, AtomicLong::get)
                .description("Pasos de la ultima pasada que agotaron su cupo de lotes")
                .register(registry);
    }

    void record(Map<Paso, Integer> movidas, int pasosConCupoAgotado) {
        movidas.forEach((paso, cuantas) -> filas.get(paso).increment(cuantas));
        this.pasosConCupoAgotado.set(pasosConCupoAgotado);
    }
}
