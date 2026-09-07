package com.vetsoftware.app.paymentgateway.infrastructure.scheduling;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import com.vetsoftware.app.paymentgateway.application.dto.PaymentCollectionBatchResult;
import com.vetsoftware.app.paymentgateway.application.port.in.RunPaymentCollectionUseCase;
import com.vetsoftware.app.paymentgateway.domain.DocumentChargeOutcome;
import com.vetsoftware.app.paymentgateway.infrastructure.gateway.WompiProperties;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * El barrido diario de cobro recurrente: documentos nuevos por cobrar y
 * reintentos vencidos, de todas las clínicas.
 *
 * <p>
 * <b>05:10 de Bogotá</b>, después de la cobranza (03:40) y de la facturación
 * recurrente (04:40) que es la que emite lo que aquí se cobra.
 *
 * <p>
 * <b>Dos vueltas de cursor independientes</b>, en el orden que fija la
 * especificación: primero los documentos nuevos
 * ({@code collectNewChargesAfter}, cursor por id), después los reintentos
 * vencidos ({@code collectDueRetries}, página de
 * {@code ListDuePaymentAttemptsUseCase}). Cada una para cuando su lote sale con
 * menos elementos que {@code batchSize} —no hay {@code SKIP
 * LOCKED} de por medio, así que un lote corto sí significa fin del trabajo.
 *
 * <p>
 * <b>{@code WOMPI_ENABLED=false} corta el barrido antes de tocar la base</b>:
 * sin pasarela configurada no hay nada que intentar, y un {@code NO_WORK}
 * silencioso es preferible a que cada documento pendiente acumule un intento
 * {@code CONFIGURATION} diario.
 */
@Component
public class PaymentCollectionJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentCollectionJob.class);
    private static final ScheduledJobCatalog JOB = ScheduledJobCatalog.PAYMENT_COLLECTION;

    private final RunPaymentCollectionUseCase worker;
    private final SystemAuthRunner systemAuthRunner;
    private final ScheduledJobTelemetry telemetry;
    private final WompiProperties wompiProperties;
    private final Clock clock;
    private final int batchSize;

    public PaymentCollectionJob(RunPaymentCollectionUseCase worker,
            SystemAuthRunner systemAuthRunner, ScheduledJobTelemetry telemetry,
            WompiProperties wompiProperties, Clock clock,
            @Value("${payment.collection.batch-size:100}") int batchSize) {
        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize must be positive");
        this.worker = worker;
        this.systemAuthRunner = systemAuthRunner;
        this.telemetry = telemetry;
        this.wompiProperties = wompiProperties;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    // lockAtMostFor cubre el peor caso, no el tipico: batchSize (100) x (sondeo
    // 6x2s + dos llamadas HTTP a 15s de readTimeout) ~ 70 min. PT2H
    // deja margen sin acercarse al ciclo diario del propio job.
    @Scheduled(cron = "${payment.collection.cron:0 10 5 * * *}", zone = ScheduledJobCatalog.ZONE)
    @SchedulerLock(name = "payment.collection", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
    public void runCollection() {
        telemetry.observe(JOB, this::executeCollection);
    }

    private Outcome executeCollection() {
        if (!wompiProperties.enabled()) {
            log.warn("Cobro recurrente omitido: Wompi está deshabilitado (WOMPI_ENABLED=false)");
            return Outcome.NO_WORK;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Map<DocumentChargeOutcome, Integer> outcomeCounts = new EnumMap<>(
                DocumentChargeOutcome.class);
        int processed = 0;
        int failures = 0;

        long cursor = 0L;
        PaymentCollectionBatchResult newBatch;
        do {
            long afterId = cursor;
            newBatch = systemAuthRunner
                    .call(() -> worker.collectNewChargesAfter(afterId, batchSize));
            processed += newBatch.processed();
            failures += newBatch.failures();
            merge(outcomeCounts, newBatch.outcomeCounts());
            if (newBatch.lastId() <= cursor) {
                break;
            }
            cursor = newBatch.lastId();
        } while (newBatch.processed() == batchSize);

        int page = 0;
        PaymentCollectionBatchResult dueBatch;
        do {
            int currentPage = page;
            dueBatch = systemAuthRunner
                    .call(() -> worker.collectDueRetries(now, currentPage, batchSize));
            processed += dueBatch.processed();
            failures += dueBatch.failures();
            merge(outcomeCounts, dueBatch.outcomeCounts());
            page++;
        } while (dueBatch.processed() == batchSize);

        if (processed == 0)
            return Outcome.NO_WORK;
        log.info("Cobro recurrente finalizado: {} candidato(s), {} fallo(s), desenlaces {}",
                processed, failures, outcomeCounts);
        return Outcome.from(processed, failures);
    }

    private static void merge(Map<DocumentChargeOutcome, Integer> total,
            Map<DocumentChargeOutcome, Integer> batch) {
        batch.forEach((outcome, count) -> total.merge(outcome, count, Integer::sum));
    }
}
