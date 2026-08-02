package com.vetsoftware.app.infrastructure.audit.chain;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Estado de la cadena de auditoría expuesto a Prometheus.
 *
 * <p>
 * {@code audit.chain.broken} es la señal crítica: una cadena rota significa que
 * algún evento fue suprimido o alterado en la base de datos. Se inicializa en
 * {@code -1} (desconocido) para que el panel no muestre "sano" antes de la
 * primera verificación.
 */
@Component
@ConditionalOnProperty(prefix = "vetsoftware.audit.outbox", name = "enabled", havingValue = "true")
public final class AuditChainMetrics {

    private final AtomicLong broken = new AtomicLong(-1);
    private final AtomicLong verifiedSequence = new AtomicLong(0);
    private final AtomicLong failureSequence = new AtomicLong(0);
    private final AtomicLong lastCheckpointSequence = new AtomicLong(0);

    AuditChainMetrics(MeterRegistry meterRegistry, AuditChainRepository repository) {
        Gauge.builder("audit.chain.broken", broken, AtomicLong::doubleValue).description(
                "1 si la última verificación encontró una divergencia, 0 si no, -1 sin verificar")
                .register(meterRegistry);
        Gauge.builder("audit.chain.verified.sequence", verifiedSequence, AtomicLong::doubleValue)
                .description("Última posición de la cadena verificada correctamente")
                .register(meterRegistry);
        Gauge.builder("audit.chain.failure.sequence", failureSequence, AtomicLong::doubleValue)
                .description(
                        "Posición donde se detectó la divergencia, 0 si la cadena está intacta")
                .register(meterRegistry);
        Gauge.builder("audit.chain.checkpoint.sequence", lastCheckpointSequence,
                AtomicLong::doubleValue)
                .description("Última posición anclada en almacenamiento inmutable")
                .register(meterRegistry);
        Gauge.builder("audit.chain.length", repository, AuditChainMetrics::chainLength)
                .description("Longitud total de la cadena emitida").register(meterRegistry);
        Gauge.builder("audit.chain.unsequenced", repository, AuditChainMetrics::unsequenced)
                .description("Eventos insertados que aún no tienen posición en la cadena")
                .register(meterRegistry);
    }

    void verified(AuditChainVerifier.Result result) {
        broken.set(result.intact() ? 0 : 1);
        verifiedSequence.set(result.lastVerifiedSequence());
        failureSequence.set(result.failureSequence());
    }

    void checkpointed(long sequence) {
        lastCheckpointSequence.set(sequence);
    }

    private static double chainLength(AuditChainRepository repository) {
        try {
            return repository.head().lastSequence();
        } catch (RuntimeException ignored) {
            return Double.NaN;
        }
    }

    private static double unsequenced(AuditChainRepository repository) {
        try {
            return repository.unsequencedCount();
        } catch (RuntimeException ignored) {
            return Double.NaN;
        }
    }
}
