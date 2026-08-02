package com.vetsoftware.app.infrastructure.audit.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "vetsoftware.audit.outbox", name = "enabled", havingValue = "true")
final class AuditOutboxMetrics {

    private final Counter published;
    private final Counter failed;

    AuditOutboxMetrics(MeterRegistry meterRegistry, AuditOutboxRepository repository) {
        published = Counter.builder("audit.outbox.published")
                .description("Eventos de auditoría aceptados por Firehose").register(meterRegistry);
        failed = Counter.builder("audit.outbox.publish.failures")
                .description("Intentos de publicación de auditoría fallidos")
                .register(meterRegistry);
        Gauge.builder("audit.outbox.pending", repository, AuditOutboxMetrics::pending)
                .description("Eventos pendientes, en proceso o fallidos").register(meterRegistry);
        Gauge.builder("audit.outbox.failed", repository, AuditOutboxMetrics::failed)
                .description("Eventos en estado FAILED").register(meterRegistry);
        Gauge.builder("audit.outbox.oldest.age", repository, AuditOutboxMetrics::oldestAge)
                .baseUnit("seconds").description("Edad del evento no publicado más antiguo")
                .register(meterRegistry);
    }

    void published(int count) {
        published.increment(count);
    }

    void failed(int count) {
        failed.increment(count);
    }

    private static double pending(AuditOutboxRepository repository) {
        try {
            return repository.pendingCount();
        } catch (RuntimeException ignored) {
            return Double.NaN;
        }
    }

    private static double failed(AuditOutboxRepository repository) {
        try {
            return repository.failedCount();
        } catch (RuntimeException ignored) {
            return Double.NaN;
        }
    }

    private static double oldestAge(AuditOutboxRepository repository) {
        try {
            return repository.oldestPendingAgeSeconds();
        } catch (RuntimeException ignored) {
            return Double.NaN;
        }
    }
}
