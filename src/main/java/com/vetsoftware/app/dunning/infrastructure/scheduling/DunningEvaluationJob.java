package com.vetsoftware.app.dunning.infrastructure.scheduling;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.dunning.application.dto.DunningBatchResult;
import com.vetsoftware.app.dunning.application.port.in.ProcessDunningBatchUseCase;
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
 */
@Component
public class DunningEvaluationJob {

    private static final Logger log = LoggerFactory.getLogger(DunningEvaluationJob.class);
    private static final String JOB_NAME = "subscription.dunning";

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

    @Scheduled(initialDelayString = "${subscription.dunning.initial-delay-ms:240000}", fixedDelayString = "${subscription.dunning.poll-delay-ms:86400000}")
    public void runDunning() {
        telemetry.observe(JOB_NAME, this::executeDunning);
    }

    private Outcome executeDunning() {
        long cursor = 0L;
        int processed = 0;
        DunningBatchResult batch;
        do {
            long afterId = cursor;
            batch = systemAuthRunner.call(() -> worker.processBatchAfter(afterId, batchSize));
            processed += batch.processed();
            cursor = batch.lastId();
        } while (batch.processed() == batchSize);

        if (processed == 0)
            return Outcome.NO_WORK;
        log.info("Cobranza de suscripciones finalizada: {} factura(s) procesada(s)", processed);
        return Outcome.SUCCESS;
    }
}
