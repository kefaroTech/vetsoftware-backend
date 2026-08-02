package com.vetsoftware.app.infrastructure.audit.chain;

import com.vetsoftware.app.infrastructure.audit.outbox.AuditEventStore;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ancla la cadena en almacenamiento inmutable.
 *
 * <p>
 * Emite periódicamente un evento con la cabeza de la cadena. Al viajar por la
 * misma ruta que el resto de la auditoría, termina en el bucket con Object Lock
 * en modo COMPLIANCE, donde ya no puede modificarse ni borrarse antes de que
 * expire la retención.
 *
 * <p>
 * Eso es lo que convierte la cadena en evidencia y no solo en una suma de
 * comprobación: para reescribir la historia habría que alterar además un objeto
 * que es inmutable incluso para la cuenta raíz. Sin el ancla, alguien con
 * acceso a la base podría recalcular toda la cadena tras manipular un evento y
 * quedaría consistente.
 *
 * <p>
 * El propio checkpoint entra en la cadena como un eslabón más, así que la
 * secuencia que reporta es la cabeza <em>anterior</em> a él.
 */
@Component
@ConditionalOnProperty(prefix = "vetsoftware.audit.outbox", name = "enabled", havingValue = "true")
final class AuditChainCheckpointJob {

    private static final Logger log = LoggerFactory.getLogger(AuditChainCheckpointJob.class);

    private final AuditChainRepository repository;
    private final AuditEventStore eventStore;
    private final AuditChainMetrics metrics;
    private final ScheduledJobTelemetry telemetry;

    AuditChainCheckpointJob(AuditChainRepository repository, AuditEventStore eventStore,
            AuditChainMetrics metrics, ScheduledJobTelemetry telemetry) {
        this.repository = repository;
        this.eventStore = eventStore;
        this.metrics = metrics;
        this.telemetry = telemetry;
    }

    @Scheduled(fixedDelayString = "${vetsoftware.audit.outbox.checkpoint-interval:PT1H}", initialDelayString = "${vetsoftware.audit.outbox.checkpoint-initial-delay:PT2M}")
    void checkpoint() {
        telemetry.observe("audit.chain.checkpoint", this::emitCheckpoint);
    }

    ScheduledJobTelemetry.Outcome emitCheckpoint() {
        AuditChainRepository.Head head = repository.head();

        // Sin eventos nuevos, un checkpoint repetido solo añadiría ruido al archivo.
        if (head.lastSequence() == 0 || head.lastSequence() == head.lastCheckpointSequence()) {
            return ScheduledJobTelemetry.Outcome.NO_WORK;
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("chain.sequence", head.lastSequence());
        attributes.put("chain.hash", head.lastChainHash());
        attributes.put("chain.previousCheckpointSequence", head.lastCheckpointSequence());

        eventStore.append("audit_chain_checkpoint", "SUCCESS", attributes);
        repository.markCheckpoint(head.lastSequence(), Instant.now());
        metrics.checkpointed(head.lastSequence());

        log.info("Checkpoint de la cadena de auditoría emitido; posición={} hash={}",
                head.lastSequence(), head.lastChainHash());
        return ScheduledJobTelemetry.Outcome.SUCCESS;
    }
}
