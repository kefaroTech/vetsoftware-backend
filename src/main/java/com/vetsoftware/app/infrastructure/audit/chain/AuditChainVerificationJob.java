package com.vetsoftware.app.infrastructure.audit.chain;

import com.vetsoftware.app.infrastructure.audit.outbox.AuditOutboxProperties;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Recorre toda la ventana retenida de la cadena en cada pasada y publica el
 * resultado como métrica.
 *
 * <p>
 * <b>La pasada es completa a propósito.</b> Una verificación incremental que
 * solo mirase los eslabones nuevos sería inútil: el ataque probable no es
 * alterar un evento futuro sino uno <em>pasado</em>, y un cursor que nunca
 * vuelve atrás jamás lo vería. La cobertura se paga con lectura y CPU, y el
 * mando para regularla es {@code verify-interval}, nunca recortar el alcance.
 *
 * <p>
 * El recorrido va por lotes para acotar la memoria, arrastrando el eslabón
 * esperado entre lotes. Como el trabajo usa {@code fixedDelay}, dos pasadas
 * nunca se solapan aunque una tarde más que el intervalo.
 */
@Component
@ConditionalOnProperty(prefix = "vetsoftware.audit.outbox", name = "enabled", havingValue = "true")
final class AuditChainVerificationJob {

    private static final Logger log = LoggerFactory.getLogger(AuditChainVerificationJob.class);

    private final AuditChainRepository repository;
    private final AuditChainMetrics metrics;
    private final ScheduledJobTelemetry telemetry;
    private final int batchSize;

    AuditChainVerificationJob(AuditChainRepository repository, AuditChainMetrics metrics,
            ScheduledJobTelemetry telemetry, AuditOutboxProperties properties) {
        this.repository = repository;
        this.metrics = metrics;
        this.telemetry = telemetry;
        this.batchSize = properties.getVerifyBatchSize();
    }

    @Scheduled(fixedDelayString = "${vetsoftware.audit.outbox.verify-interval:PT5M}", initialDelayString = "${vetsoftware.audit.outbox.verify-initial-delay:PT1M}")
    void verify() {
        telemetry.observe("audit.chain.verify", this::verifySweep);
    }

    ScheduledJobTelemetry.Outcome verifySweep() {
        List<AuditChainRepository.Link> batch = repository.linksAfter(0, batchSize);
        if (batch.isEmpty()) {
            return ScheduledJobTelemetry.Outcome.NO_WORK;
        }

        // Los eslabones anteriores al primero retenido ya fueron anclados en el archivo
        // inmutable:
        // no se pueden recalcular aquí, así que su enlace se toma como punto de
        // partida.
        long expectedSequence = batch.getFirst().sequence();
        String expectedPreviousHash = batch.getFirst().previousHash();
        int totalChecked = 0;

        while (!batch.isEmpty()) {
            AuditChainVerifier.Result result = AuditChainVerifier.verify(batch, expectedSequence,
                    expectedPreviousHash);
            totalChecked += result.checkedCount();

            if (!result.intact()) {
                metrics.verified(result);
                log.error(
                        "CADENA DE AUDITORÍA ROTA en la posición {}: {}. Última posición válida: {}. "
                                + "Contrastar contra el archivo inmutable ANTES de tocar la base de datos.",
                        result.failureSequence(), result.failureReason(),
                        result.lastVerifiedSequence());
                return ScheduledJobTelemetry.Outcome.FAILURE;
            }

            expectedSequence = result.lastVerifiedSequence() + 1;
            expectedPreviousHash = result.lastVerifiedHash();
            batch = repository.linksAfter(result.lastVerifiedSequence(), batchSize);
        }

        metrics.verified(new AuditChainVerifier.Result(true, totalChecked, expectedSequence - 1,
                expectedPreviousHash, 0, null));
        return ScheduledJobTelemetry.Outcome.SUCCESS;
    }
}
