package com.vetsoftware.app.infrastructure.audit.outbox;

import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "vetsoftware.audit.outbox",
        name = "enabled",
        havingValue = "true")
final class AuditOutboxCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AuditOutboxCleanupJob.class);

    private final AuditOutboxRepository repository;
    private final AuditOutboxProperties properties;
    private final ScheduledJobTelemetry telemetry;

    AuditOutboxCleanupJob(
            AuditOutboxRepository repository,
            AuditOutboxProperties properties,
            ScheduledJobTelemetry telemetry) {
        this.repository = repository;
        this.properties = properties;
        this.telemetry = telemetry;
    }

    @Scheduled(
            fixedDelayString = "${vetsoftware.audit.outbox.cleanup-interval:PT24H}",
            initialDelayString = "${vetsoftware.audit.outbox.cleanup-initial-delay:PT1H}")
    void cleanup() {
        telemetry.observe("audit.outbox.cleanup", () -> {
            int deleted = repository.deletePublishedBefore(
                    Instant.now().minus(properties.getRetention()),
                    properties.getCleanupBatchSize());
            if (deleted > 0) {
                log.info("Outbox de auditoría depurada; publicados eliminados={}", deleted);
            }
            return deleted == 0
                    ? ScheduledJobTelemetry.Outcome.NO_WORK
                    : ScheduledJobTelemetry.Outcome.SUCCESS;
        });
    }
}
