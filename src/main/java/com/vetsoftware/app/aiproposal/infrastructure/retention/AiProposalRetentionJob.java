package com.vetsoftware.app.aiproposal.infrastructure.retention;

import com.vetsoftware.app.aiproposal.application.port.out.ProposalRetentionPort;
import com.vetsoftware.app.aiproposal.infrastructure.retention.AiProposalRetentionMetrics.Paso;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * La politica de retencion de {@code aiproposal}: anonimizar, redactar y
 * purgar.
 *
 * <p>
 * <strong>Seis pasos y los seis por lotes.</strong> Tres anonimizan -cabecera,
 * turnos y motivos- y tres purgan, en el orden que exigen las FK
 * {@code ON DELETE RESTRICT}: lineas, turnos, cabecera. Los seis son
 * idempotentes y estan encadenados por el estado de la fila, no por una lista
 * de ids que haya que pasarse de un paso al siguiente: si el proceso muere
 * entre el paso 1 y el 2, la pasada siguiente recoge exactamente lo que quedo a
 * medias.
 *
 * <p>
 * &#9940; <strong>Este job informa PARTIAL_FAILURE, y ahi esta casi todo su
 * valor.</strong> Un barrido que informa exito mientras no hace nada es el
 * defecto que este proyecto ya encontro dos veces en los jobs de facturacion,
 * donde una terminacion escrita como {@code == batchSize} dejaba clientes que
 * pagaban sin acceso y el job en verde. Aqui hay <em>dos</em> formas distintas
 * de no terminar, y las dos se informan como {@code PARTIAL_FAILURE} en vez de
 * como exito:
 *
 * <ul>
 * <li><strong>Un paso reventó.</strong> No aborta la pasada: los seis se
 * intentan siempre y el fallo de uno no puede impedir que corran los otros
 * cinco. Si revientan los seis es {@code FAILURE}.</li>
 * <li><strong>Un paso agoto su cupo de lotes</strong>, es decir todos sus lotes
 * volvieron llenos. Eso significa que quedan filas elegibles sin tratar y que
 * la pasada <em>no</em> completo la politica. Informarlo como exito es como se
 * llega a una tabla con correos de hace ocho meses y un panel en verde.</li>
 * </ul>
 *
 * <p>
 * <strong>{@code NO_WORK} se reserva para el caso honesto:</strong> los seis
 * pasos corrieron, ninguno fallo, ninguno agoto cupo y no habia ni una fila
 * elegible. Esa es la noche normal. No se usa
 * {@code ScheduledJobTelemetry.Outcome.from(intentados, fallos)} porque ahi
 * "intentados" son siempre seis y nunca daria {@code NO_WORK}; la distincion
 * util no es cuantos pasos corrieron sino cuantas filas se movieron.
 *
 * <p>
 * <strong>Sin {@code @Transactional} en ninguna parte de esta clase.</strong>
 * Cada lote abre y cierra la suya dentro del adaptador: asi los bloqueos duran
 * un lote, un fallo tardio conserva lo ya hecho, y la pasada no mantiene las
 * tres tablas tomadas durante minutos.
 */
@Component
@ConditionalOnProperty(prefix = "vetsoftware.ai.proposal.retention", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiProposalRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AiProposalRetentionJob.class);

    private final ProposalRetentionPort retention;

    private final AiProposalRetentionProperties properties;

    private final AiProposalRetentionMetrics metrics;

    private final ScheduledJobTelemetry telemetry;

    private final Clock clock;

    public AiProposalRetentionJob(ProposalRetentionPort retention,
            AiProposalRetentionProperties properties, AiProposalRetentionMetrics metrics,
            ScheduledJobTelemetry telemetry, Clock clock) {
        properties.validate();
        this.retention = retention;
        this.properties = properties;
        this.metrics = metrics;
        this.telemetry = telemetry;
        this.clock = clock;
    }

    @Scheduled(cron = "${vetsoftware.ai.proposal.retention.cron:0 55 3 * * *}", zone = ScheduledJobCatalog.ZONE)
    void barrer() {
        telemetry.observe(ScheduledJobCatalog.AI_PROPOSAL_RETENTION, this::aplicarRetencion);
    }

    ScheduledJobTelemetry.Outcome aplicarRetencion() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime inactivasDesde = ahora.minus(properties.getAnonymizeAfter());
        LocalDateTime purgablesDesde = ahora.minus(properties.getPurgeAfter());
        int lote = properties.getBatchSize();

        Pasada pasada = new Pasada();
        pasada.ejecutar(Paso.ANONIMIZAR_PROPUESTAS,
                () -> retention.anonymizeProposals(inactivasDesde, ahora, lote));
        pasada.ejecutar(Paso.REDACTAR_TURNOS, () -> retention.redactTurns(lote));
        pasada.ejecutar(Paso.REDACTAR_MOTIVOS, () -> retention.redactLineReasons(ahora, lote));
        pasada.ejecutar(Paso.PURGAR_LINEAS, () -> retention.purgeLines(purgablesDesde, lote));
        pasada.ejecutar(Paso.PURGAR_TURNOS, () -> retention.purgeTurns(purgablesDesde, lote));
        // ANTES de purgar la cabecera, y ese orden es todo el paso: la aceptacion
        // apunta a la propuesta por su id en un VARCHAR sin clave foranea, asi que
        // borrar primero la cabecera deja una fila que ya no se puede reconocer.
        pasada.ejecutar(Paso.PURGAR_ACEPTACIONES,
                () -> retention.purgeAcceptances(purgablesDesde, lote));
        pasada.ejecutar(Paso.PURGAR_PROPUESTAS,
                () -> retention.purgeProposals(purgablesDesde, lote));

        metrics.record(pasada.movidas, pasada.conCupoAgotado);
        pasada.registrar();
        return pasada.desenlace();
    }

    /**
     * El estado de una pasada. Clase interna y no seis variables sueltas porque el
     * desenlace depende de las tres a la vez, y calcularlo con contadores
     * repartidos por el metodo es como se escribe un job que informa exito sin
     * haberlo tenido.
     */
    private final class Pasada {

        private final Map<Paso, Integer> movidas = new EnumMap<>(Paso.class);

        private int fallos;

        private int conCupoAgotado;

        void ejecutar(Paso paso, IntSupplier unLote) {
            try {
                int total = 0;
                for (int intento = 0; intento < properties.getMaxBatchesPerRun(); intento++) {
                    int filas = unLote.getAsInt();
                    total += filas;
                    if (filas < properties.getBatchSize()) {
                        movidas.put(paso, total);
                        return;
                    }
                }
                // Todos los lotes volvieron llenos: el cupo se agoto y quedan filas
                // elegibles sin tratar. NO es exito, aunque haya movido miles.
                conCupoAgotado++;
                movidas.put(paso, total);
            } catch (RuntimeException fallo) {
                fallos++;
                movidas.putIfAbsent(paso, 0);
                // Sin el correo, sin el texto y sin ids: lo que se esta limpiando aqui
                // es justamente lo que no puede acabar en un log con 31 dias de
                // retencion. El nombre del paso y la excepcion bastan para depurar.
                log.error("Fallo el paso {} de la retencion de propuestas", paso.etiqueta(), fallo);
            }
        }

        void registrar() {
            long total = movidas.values().stream().mapToLong(Integer::longValue).sum();
            if (total > 0) {
                log.info(
                        "Retencion de propuestas; anonimizadas={}, turnos={}, motivos={},"
                                + " lineas purgadas={}, turnos purgados={}, propuestas purgadas={}",
                        cuenta(Paso.ANONIMIZAR_PROPUESTAS), cuenta(Paso.REDACTAR_TURNOS),
                        cuenta(Paso.REDACTAR_MOTIVOS), cuenta(Paso.PURGAR_LINEAS),
                        cuenta(Paso.PURGAR_TURNOS), cuenta(Paso.PURGAR_PROPUESTAS));
            }
            if (conCupoAgotado > 0) {
                log.warn(
                        "La retencion de propuestas agoto su cupo en {} de {} pasos: quedan filas"
                                + " sin tratar. Subir max-batches-per-run o el barrido pierde"
                                + " terreno contra el ritmo de entrada",
                        conCupoAgotado, Paso.values().length);
            }
        }

        int cuenta(Paso paso) {
            return movidas.getOrDefault(paso, 0);
        }

        ScheduledJobTelemetry.Outcome desenlace() {
            if (fallos == Paso.values().length) {
                return ScheduledJobTelemetry.Outcome.FAILURE;
            }
            if (fallos > 0 || conCupoAgotado > 0) {
                return ScheduledJobTelemetry.Outcome.PARTIAL_FAILURE;
            }
            return movidas.values().stream().mapToLong(Integer::longValue).sum() == 0
                    ? ScheduledJobTelemetry.Outcome.NO_WORK
                    : ScheduledJobTelemetry.Outcome.SUCCESS;
        }
    }
}
