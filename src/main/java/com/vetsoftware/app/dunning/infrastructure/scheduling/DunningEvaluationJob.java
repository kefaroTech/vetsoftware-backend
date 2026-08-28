package com.vetsoftware.app.dunning.infrastructure.scheduling;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.dunning.application.dto.DunningBatchResult;
import com.vetsoftware.app.dunning.application.port.in.ProcessDunningBatchUseCase;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Barrido diario de facturas externas vencidas con saldo pendiente.
 *
 * <p>
 * <b>Depende del puerto, no del bean concreto, y sigue entrando bajo
 * {@link SystemAuthRunner}.</b> Un proceso programado no tiene principal: sin
 * el runner no habria autenticacion que satisficiera el
 * {@code hasRole('SYSTEM')} que el puerto exige, y el barrido se caeria cada
 * noche con un {@code AccessDeniedException}. Pasar por el puerto es lo que
 * hace que ese gate se evalue de verdad.
 *
 * <h2>Por que un lote corto NO cierra el barrido (#468)</h2>
 *
 * <p>
 * La consulta que alimenta cada vuelta es
 * {@code ... AND id > :afterId ORDER BY id LIMIT :batchSize FOR UPDATE SKIP
 * LOCKED}. <b>{@code SKIP LOCKED} descarta en silencio las filas que otra
 * transaccion tiene bloqueadas</b>, asi que un lote de menos de
 * {@code batchSize} significa «habia filas ocupadas», no «se acabo el trabajo».
 * Con dos instancias en ECS —el despliegue normal— la segunda recibe un lote
 * corto en su primera vuelta, y con la condicion vieja
 * ({@code while (batch.processed() == batchSize)}) daba el barrido por
 * terminado: esa noche no se evaluaba ninguna factura con id posterior. Lo que
 * duele no es que un moroso tarde un dia mas en bajar a solo lectura, sino que
 * <b>un cliente que ya pago se queda 24 horas mas en solo lectura</b> —la
 * reactivacion la hace este mismo barrido, en
 * {@code DunningEvaluationService.reactivateIfNeeded}— y el job reporta verde.
 *
 * <h2>Por que la condicion de parada nueva termina siempre</h2>
 *
 * <p>
 * Son tres salidas, y basta con la segunda para que el bucle sea finito pase lo
 * que pase:
 *
 * <ol>
 * <li><b>Lote vacio</b> ({@code processed == 0}): fin natural, y la unica
 * salida que significa «no queda nada».</li>
 * <li><b>El cursor no avanzo</b> ({@code lastId <= afterId}): imposible con el
 * adaptador real —la consulta filtra {@code id > :afterId} y ordena por
 * {@code id}, asi que el {@code lastId} de un lote no vacio supera siempre al
 * cursor de entrada—, pero es lo que convierte la terminacion en una propiedad
 * de <em>este</em> metodo y no en una confianza depositada en el repositorio.
 * Sin esta guarda, una implementacion que devolviera filas sin mover el cursor
 * giraria para siempre bajo un {@code @Scheduled}, reteniendo un hilo del pool
 * y una conexion por vuelta.</li>
 * <li><b>Tope de vueltas</b> ({@link #MAX_VUELTAS}): con el cursor avanzando de
 * forma estrictamente monotona el bucle ya termina —los ids son finitos—, pero
 * nada impide que se inserten filas nuevas mientras el barrido corre. El tope
 * acota la ejecucion a {@code MAX_VUELTAS * batchSize} filas por noche.</li>
 * </ol>
 *
 * <p>
 * Las dos salidas defensivas <b>no se reportan como exito</b>: devuelven
 * {@link Outcome#PARTIAL_FAILURE}, que sella el heartbeat —el job corrio— pero
 * deja la señal distinta de {@code success} en la metrica. Un barrido truncado
 * que se reporta verde es exactamente el defecto que este cambio arregla; no
 * tiene sentido cerrar una via y dejar la otra.
 */
@Component
public class DunningEvaluationJob {

    private static final Logger log = LoggerFactory.getLogger(DunningEvaluationJob.class);
    private static final ScheduledJobCatalog JOB = ScheduledJobCatalog.SUBSCRIPTION_DUNNING;

    /**
     * Techo duro de vueltas por ejecucion. Con el {@code batch-size} por defecto
     * (100) son 1.000.000 de facturas por noche: no acota ningun volumen real, solo
     * impide que una insercion continua mantenga vivo el barrido indefinidamente.
     */
    private static final int MAX_VUELTAS = 10_000;

    private final ProcessDunningBatchUseCase worker;
    private final SystemAuthRunner systemAuthRunner;
    private final ScheduledJobTelemetry telemetry;
    private final int batchSize;

    public DunningEvaluationJob(ProcessDunningBatchUseCase worker,
            SystemAuthRunner systemAuthRunner, ScheduledJobTelemetry telemetry,
            @Value("${subscription.dunning.batch-size:100}") int batchSize) {
        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize must be positive");
        this.worker = worker;
        this.systemAuthRunner = systemAuthRunner;
        this.telemetry = telemetry;
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "${subscription.dunning.cron:0 40 3 * * *}", zone = ScheduledJobCatalog.ZONE)
    public void runDunning() {
        telemetry.observe(JOB, this::executeDunning);
    }

    private Outcome executeDunning() {
        long cursor = 0L;
        int processed = 0;
        int vueltas = 0;
        boolean quedaTrabajo = true;
        boolean truncado = false;
        while (quedaTrabajo) {
            long afterId = cursor;
            DunningBatchResult batch = systemAuthRunner
                    .call(() -> worker.processBatchAfter(afterId, batchSize));
            processed += batch.processed();
            vueltas++;
            if (batch.processed() == 0) {
                quedaTrabajo = false;
            } else if (batch.lastId() <= afterId) {
                log.error(
                        "Cobranza abortada: el lote devolvio {} factura(s) sin mover el cursor"
                                + " ({} -> {}). Quedan facturas sin evaluar esta noche.",
                        batch.processed(), afterId, batch.lastId());
                truncado = true;
                quedaTrabajo = false;
            } else {
                cursor = batch.lastId();
                if (vueltas >= MAX_VUELTAS) {
                    log.error(
                            "Cobranza detenida en el tope de {} vueltas con el cursor en {}."
                                    + " Quedan facturas sin evaluar esta noche.",
                            MAX_VUELTAS, cursor);
                    truncado = true;
                    quedaTrabajo = false;
                }
            }
        }

        // Sin rama para «truncado con cero procesadas»: las dos salidas defensivas
        // solo se alcanzan desde un lote NO vacio, asi que truncado implica
        // processed > 0 y esa combinacion no existe.
        if (processed == 0)
            return Outcome.NO_WORK;
        log.info("Cobranza de suscripciones finalizada: {} factura(s) procesada(s)", processed);
        return truncado ? Outcome.PARTIAL_FAILURE : Outcome.SUCCESS;
    }
}
