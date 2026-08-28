package com.vetsoftware.app.subscription.infrastructure.scheduling;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import com.vetsoftware.app.subscription.application.dto.SubscriptionLifecycleBatchResult;
import com.vetsoftware.app.subscription.application.usecase.SubscriptionLifecycleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Barrido diario de cancelaciones, fin de trial y vigencias de lineas.
 *
 * <h2>Por que un lote corto NO cierra el barrido (#468)</h2>
 *
 * <p>
 * Su lote sale de {@code SubscriptionJpaRepository.lockLifecycleBatchAfter},
 * que termina en {@code LIMIT :batchSize FOR UPDATE SKIP LOCKED}.
 * <b>{@code SKIP LOCKED} descarta en silencio las filas que otra transaccion
 * tiene bloqueadas</b>, asi que un lote de menos de {@code batchSize} significa
 * «habia contratos ocupados», no «se acabo el trabajo». Con la condicion vieja
 * ({@code while (batch.processed() == batchSize)}) la segunda instancia de ECS
 * cerraba el barrido en su primera vuelta y los contratos con id posterior se
 * quedaban un dia entero sin procesar: una baja pedida no surtia efecto y un
 * contrato en prueba no pasaba a activo, con el job reportando verde.
 *
 * <h2>Por que la condicion de parada nueva termina siempre</h2>
 *
 * <p>
 * Mismas tres salidas que en {@code DunningEvaluationJob}, y por las mismas
 * razones:
 *
 * <ol>
 * <li><b>Lote vacio</b> ({@code processed == 0}): fin natural.</li>
 * <li><b>El cursor no avanzo</b> ({@code lastId <= afterId}): imposible con el
 * adaptador real —la consulta filtra {@code s.id > :afterId} y ordena por
 * {@code s.id}—, pero hace que la terminacion sea una propiedad de este metodo
 * y no una confianza depositada en el repositorio.</li>
 * <li><b>Tope de vueltas</b> ({@link #MAX_VUELTAS}), que acota la ejecucion
 * aunque entren contratos nuevos mientras el barrido corre.</li>
 * </ol>
 *
 * <p>
 * Las dos salidas defensivas devuelven {@link Outcome#PARTIAL_FAILURE}: sellan
 * el heartbeat —el job corrio— pero no mienten diciendo {@code success}.
 */
@Component
public class SubscriptionLifecycleJob {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionLifecycleJob.class);
    private static final ScheduledJobCatalog JOB = ScheduledJobCatalog.SUBSCRIPTION_LIFECYCLE;

    /**
     * Techo duro de vueltas por ejecucion. Con el {@code batch-size} por defecto
     * (100) son 1.000.000 de contratos por dia: no acota ningun volumen real, solo
     * impide que una insercion continua mantenga vivo el barrido indefinidamente.
     */
    private static final int MAX_VUELTAS = 10_000;

    private final SubscriptionLifecycleWorker worker;
    private final SystemAuthRunner systemAuthRunner;
    private final ScheduledJobTelemetry telemetry;
    private final int batchSize;

    public SubscriptionLifecycleJob(SubscriptionLifecycleWorker worker,
            SystemAuthRunner systemAuthRunner, ScheduledJobTelemetry telemetry,
            @Value("${subscription.lifecycle.batch-size:100}") int batchSize) {
        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize must be positive");
        this.worker = worker;
        this.systemAuthRunner = systemAuthRunner;
        this.telemetry = telemetry;
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "${subscription.lifecycle.cron:0 10 3 * * *}", zone = ScheduledJobCatalog.ZONE)
    public void runLifecycle() {
        telemetry.observe(JOB, this::executeLifecycle);
    }

    private Outcome executeLifecycle() {
        long cursor = 0L;
        int processed = 0;
        int vueltas = 0;
        boolean quedaTrabajo = true;
        boolean truncado = false;
        while (quedaTrabajo) {
            long afterId = cursor;
            SubscriptionLifecycleBatchResult batch = systemAuthRunner
                    .call(() -> worker.processBatchAfter(afterId, batchSize));
            processed += batch.processed();
            vueltas++;
            if (batch.processed() == 0) {
                quedaTrabajo = false;
            } else if (batch.lastId() <= afterId) {
                log.error(
                        "Lifecycle abortado: el lote devolvio {} contrato(s) sin mover el cursor"
                                + " ({} -> {}). Quedan contratos sin procesar hoy.",
                        batch.processed(), afterId, batch.lastId());
                truncado = true;
                quedaTrabajo = false;
            } else {
                cursor = batch.lastId();
                if (vueltas >= MAX_VUELTAS) {
                    log.error("Lifecycle detenido en el tope de {} vueltas con el cursor en {}."
                            + " Quedan contratos sin procesar hoy.", MAX_VUELTAS, cursor);
                    truncado = true;
                    quedaTrabajo = false;
                }
            }
        }

        // Sin rama para «truncado con cero procesados»: las dos salidas defensivas
        // solo se alcanzan desde un lote NO vacio, asi que truncado implica
        // processed > 0 y esa combinacion no existe.
        if (processed == 0)
            return Outcome.NO_WORK;
        log.info("Lifecycle de suscripciones finalizado: {} contrato(s) procesado(s)", processed);
        return truncado ? Outcome.PARTIAL_FAILURE : Outcome.SUCCESS;
    }
}
