package com.vetsoftware.app.paymentgateway.infrastructure.scheduling;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import com.vetsoftware.app.paymentgateway.application.dto.ReconciliationBatchResult;
import com.vetsoftware.app.paymentgateway.application.port.in.ReconcilePendingPaymentsUseCase;
import com.vetsoftware.app.paymentgateway.infrastructure.gateway.WompiProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consulta a Wompi los {@code PENDING} envejecidos; ver
 * {@code ReconcilePendingPaymentsService}.
 */
@Component
public class PaymentReconciliationJob {

    /**
     * Sin cursor, {@code reconcileOlderThan} vuelve a consultar la misma ventana en
     * cada pasada, así que solo avanza si algo se resolvió. Un pago marcado
     * envejecido se queda {@code PENDING} a propósito (ver
     * {@code ReconcilePendingPaymentsService}) y puede seguir saliendo como
     * candidato sin resolverse nunca en esta corrida; el tope evita que eso
     * convierta el barrido en un bucle sin fin.
     */
    static final int MAX_ITERATIONS = 20;

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationJob.class);
    private static final ScheduledJobCatalog JOB = ScheduledJobCatalog.PAYMENT_RECONCILIATION;

    private final ReconcilePendingPaymentsUseCase worker;
    private final SystemAuthRunner systemAuthRunner;
    private final ScheduledJobTelemetry telemetry;
    private final WompiProperties wompiProperties;
    private final Clock clock;
    private final Duration staleAfter;
    private final int batchSize;

    public PaymentReconciliationJob(ReconcilePendingPaymentsUseCase worker,
            SystemAuthRunner systemAuthRunner, ScheduledJobTelemetry telemetry,
            WompiProperties wompiProperties, Clock clock,
            @Value("${payment.reconciliation.stale-minutes:60}") long staleMinutes,
            @Value("${payment.reconciliation.batch-size:100}") int batchSize) {
        if (staleMinutes <= 0)
            throw new IllegalArgumentException("staleMinutes must be positive");
        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize must be positive");
        this.worker = worker;
        this.systemAuthRunner = systemAuthRunner;
        this.telemetry = telemetry;
        this.wompiProperties = wompiProperties;
        this.clock = clock;
        this.staleAfter = Duration.ofMinutes(staleMinutes);
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "${payment.reconciliation.cron:0 0 * * * *}", zone = ScheduledJobCatalog.ZONE)
    @SchedulerLock(name = "payment.reconciliation", lockAtMostFor = "PT50M", lockAtLeastFor = "PT1M")
    public void runReconciliation() {
        telemetry.observe(JOB, this::executeReconciliation);
    }

    private Outcome executeReconciliation() {
        if (!wompiProperties.enabled()) {
            log.warn("Conciliación omitida: Wompi está deshabilitado (WOMPI_ENABLED=false)");
            return Outcome.NO_WORK;
        }

        LocalDateTime cutoff = LocalDateTime.now(clock).minus(staleAfter);
        int totalProcessed = 0;
        int totalResolved = 0;
        int totalFailures = 0;
        int iteration = 0;
        ReconciliationBatchResult batch;
        do {
            batch = systemAuthRunner.call(() -> worker.reconcileOlderThan(cutoff, batchSize));
            totalProcessed += batch.processed();
            totalResolved += batch.resolved();
            totalFailures += batch.failures();
            iteration++;
        } while (batch.processed() == batchSize && iteration < MAX_ITERATIONS);

        if (totalProcessed == 0) {
            return Outcome.NO_WORK;
        }
        log.info("Conciliación finalizada: {} candidato(s), {} resuelto(s), {} fallo(s)",
                totalProcessed, totalResolved, totalFailures);
        return Outcome.from(totalProcessed, totalFailures);
    }
}
