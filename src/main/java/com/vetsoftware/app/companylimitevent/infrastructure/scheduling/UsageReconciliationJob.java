package com.vetsoftware.app.companylimitevent.infrastructure.scheduling;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companylimitevent.application.dto.UsageReconciliationDto;
import com.vetsoftware.app.companylimitevent.application.port.in.ReconcileCompanyUsageUseCase;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * El recuento periodico que R-LIMIT-30 exige y que no existia.
 *
 * <p>
 * <strong>Es lo que convierte la admision en salvaguarda.</strong> El modelo
 * dice por escrito que el contador es una cache que puede desviarse y que
 * ninguna restriccion del motor puede demostrar que cuadra; sin este barrido
 * esa frase era una excusa. {@code usage_reconciled_at} y su indice
 * {@code ix_company_capacities_unreconciled} existen desde el changeset 314
 * justo para esta consulta, y su valor iba a ser {@code null} para siempre.
 *
 * <p>
 * <strong>Entra bajo {@link SystemAuthRunner}</strong>: el puerto va cerrado a
 * {@code hasRole('SYSTEM')} --recorre los contadores de todas las empresas-- y
 * un hilo del planificador no trae principal, asi que sin el runner el barrido
 * moriria cada noche con {@code AccessDeniedException}.
 *
 * <p>
 * <strong>Sin cerrojo distribuido, y la precondicion esta declarada</strong>:
 * el catalogo lo marca como {@code requiresSingleWriter}, igual que los otros
 * tres que recorren una tabla con cursor, y la alerta
 * {@code VetSoftwareScheduledJobMultipleReplicas} dispara si alguien escala a
 * dos tareas. Aqui el solape no corrompe nada --nadie escribe el contador--
 * pero si duplicaria los hechos de desvio, y esa bitacora es probatoria.
 */
@Component
public class UsageReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(UsageReconciliationJob.class);
    private static final ScheduledJobCatalog JOB = ScheduledJobCatalog.USAGE_RECONCILIATION;

    private final ReconcileCompanyUsageUseCase reconcile;
    private final SystemAuthRunner systemAuthRunner;
    private final ScheduledJobTelemetry telemetry;
    private final Clock clock;
    private final int batchSize;
    private final Duration recheckAfter;

    public UsageReconciliationJob(ReconcileCompanyUsageUseCase reconcile,
            SystemAuthRunner systemAuthRunner, ScheduledJobTelemetry telemetry, Clock clock,
            @Value("${usage.reconciliation.batch-size:200}") int batchSize,
            @Value("${usage.reconciliation.recheck-after-hours:168}") long recheckAfterHours) {
        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize must be positive");
        if (recheckAfterHours <= 0)
            throw new IllegalArgumentException("recheckAfterHours must be positive");
        this.reconcile = reconcile;
        this.systemAuthRunner = systemAuthRunner;
        this.telemetry = telemetry;
        this.clock = clock;
        this.batchSize = batchSize;
        this.recheckAfter = Duration.ofHours(recheckAfterHours);
    }

    @Scheduled(cron = "${usage.reconciliation.cron:0 10 4 * * *}", zone = ScheduledJobCatalog.ZONE)
    public void runReconciliation() {
        telemetry.observe(JOB, this::reconcileUsage);
    }

    /**
     * Repite mientras el lote salga lleno: el tope acota la transaccion, no el
     * trabajo. Pararse en el primer lote dejaria la cola creciendo mas rapido de lo
     * que se drena, y los contadores nunca comprobados --que van primero-- taparian
     * para siempre a los demas.
     *
     * <p>
     * La ventana por defecto es de una semana: recontar a diario todos los
     * contadores de la plataforma es trabajo que no responde ninguna pregunta
     * nueva, porque un desvio no se arregla solo. Los que nunca se han comprobado
     * entran siempre, sea cual sea la ventana.
     */
    private Outcome reconcileUsage() {
        LocalDateTime staleBefore = LocalDateTime.now(clock).minus(recheckAfter);
        long cursor = 0L;
        int examined = 0;
        int drifted = 0;
        int skipped = 0;
        UsageReconciliationDto batch;
        do {
            long afterId = cursor;
            batch = systemAuthRunner.call(() -> reconcile.execute(staleBefore, afterId, batchSize));
            examined += batch.examined();
            drifted += batch.drifted();
            skipped += batch.skipped();
            cursor = batch.lastId();
        } while (batch.isFullBatch(batchSize));

        if (examined == 0)
            return Outcome.NO_WORK;
        log.info("Recuento de consumo: {} contador(es) examinado(s), {} con desvio, {} sin fuente"
                + " de verdad computable", examined, drifted, skipped);
        return Outcome.SUCCESS;
    }
}
