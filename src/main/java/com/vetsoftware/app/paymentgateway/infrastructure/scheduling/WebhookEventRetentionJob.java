package com.vetsoftware.app.paymentgateway.infrastructure.scheduling;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import com.vetsoftware.app.paymentgateway.application.port.in.PurgeExpiredWebhookEventsUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vacía {@code raw_body} de los webhooks de Wompi recibidos hace más de
 * {@code vetsoftware.payments.wompi.webhook-retention-days} días: el resto de
 * la fila —desenlace, checksum, referencia— es evidencia contable que se
 * conserva indefinidamente; el cuerpo crudo no.
 *
 * <p>
 * Escritor único, con candado propio: es una sola {@code UPDATE} masiva por
 * antigüedad de llegada, y dos réplicas a la vez competirían por las mismas
 * filas sin ganar nada.
 */
@Component
public class WebhookEventRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventRetentionJob.class);
    private static final ScheduledJobCatalog JOB = ScheduledJobCatalog.WEBHOOK_EVENT_RETENTION;

    private final PurgeExpiredWebhookEventsUseCase worker;
    private final SystemAuthRunner systemAuthRunner;
    private final ScheduledJobTelemetry telemetry;
    private final Clock clock;
    private final int retentionDays;

    public WebhookEventRetentionJob(PurgeExpiredWebhookEventsUseCase worker,
            SystemAuthRunner systemAuthRunner, ScheduledJobTelemetry telemetry, Clock clock,
            @Value("${vetsoftware.payments.wompi.webhook-retention-days:90}") int retentionDays) {
        if (retentionDays <= 0)
            throw new IllegalArgumentException("retentionDays must be positive");
        this.worker = worker;
        this.systemAuthRunner = systemAuthRunner;
        this.telemetry = telemetry;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${vetsoftware.payments.wompi.webhook-retention.cron:0 5 4 * * *}", zone = ScheduledJobCatalog.ZONE)
    @SchedulerLock(name = "webhook.event.retention", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void purgeExpiredRawBodies() {
        telemetry.observe(JOB, this::execute);
    }

    private Outcome execute() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retentionDays);
        int purged = systemAuthRunner.call(() -> worker.purgeRawBodyOlderThan(cutoff));
        if (purged == 0) {
            return Outcome.NO_WORK;
        }
        log.info("Retención de webhooks de Wompi: {} fila(s) sin cuerpo crudo desde ahora", purged);
        return Outcome.SUCCESS;
    }
}
