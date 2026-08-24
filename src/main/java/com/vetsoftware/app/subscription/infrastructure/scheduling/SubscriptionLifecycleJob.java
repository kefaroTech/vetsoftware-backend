package com.vetsoftware.app.subscription.infrastructure.scheduling;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import com.vetsoftware.app.subscription.application.dto.SubscriptionLifecycleBatchResult;
import com.vetsoftware.app.subscription.application.usecase.SubscriptionLifecycleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Barrido diario de cancelaciones, fin de trial y vigencias de lineas. */
@Component
public class SubscriptionLifecycleJob {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionLifecycleJob.class);
    private static final String JOB_NAME = "subscription.lifecycle";

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

    @Scheduled(initialDelayString = "${subscription.lifecycle.initial-delay-ms:180000}", fixedDelayString = "${subscription.lifecycle.poll-delay-ms:86400000}")
    public void runLifecycle() {
        telemetry.observe(JOB_NAME, this::executeLifecycle);
    }

    private Outcome executeLifecycle() {
        long cursor = 0L;
        int processed = 0;
        SubscriptionLifecycleBatchResult batch;
        do {
            long afterId = cursor;
            batch = systemAuthRunner.call(() -> worker.processBatchAfter(afterId, batchSize));
            processed += batch.processed();
            cursor = batch.lastId();
        } while (batch.processed() == batchSize);

        if (processed == 0)
            return Outcome.NO_WORK;
        log.info("Lifecycle de suscripciones finalizado: {} contrato(s) procesado(s)", processed);
        return Outcome.SUCCESS;
    }
}
