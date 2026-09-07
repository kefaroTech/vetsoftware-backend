package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.dto.ReconciliationBatchResult;
import com.vetsoftware.app.paymentgateway.application.port.in.ReconcilePendingPaymentsUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics.FailureKind;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayPort;
import com.vetsoftware.app.paymentgateway.application.port.out.StalePendingPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransaction;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.paymentgateway.domain.RetrySchedule;
import com.vetsoftware.app.paymentgateway.domain.StalePendingPayment;
import com.vetsoftware.app.paymentgateway.domain.WompiGatewayException;
import com.vetsoftware.app.paymentgateway.domain.WompiRateLimitedException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Conciliación de pagos {@code PENDING} de pasarela envejecidos: ni el webhook
 * ni el sondeo de {@code GatewayCharger} los cerraron. La mayoría tiene una
 * transacción real de Wompi que consultar, y basta con volver a preguntarle su
 * estado y pasarlo por {@link GatewayOutcomeSettler}, que ya es idempotente si
 * el webhook ganó la carrera entre tanto. La excepción es la reserva sin
 * referencia: {@code GatewayCharger.charge} la registra antes del
 * {@code POST /transactions}, así que si ese POST nunca respondió se busca la
 * transacción por {@code client_request_id} antes de darla por fallida.
 *
 * <p>
 * <strong>Un {@code PENDING} envejecido nunca se marca {@code FAILED}.</strong>
 * Wompi puede seguir resolviéndolo después del corte de antigüedad, y confirmar
 * un pago que ya se dio por fallido dejaría el webhook sin transición posible
 * ({@code FAILED} es terminal). Por eso {@link #failIfPendingTooLong} solo
 * anota un intento {@code SOFT} —con la escalera real de {@link RetrySchedule},
 * no siempre el primer peldaño— y dice al documento que ya puede reintentar:
 * {@code PendingPaymentQueryPort} deja de contar como bloqueante un
 * {@code PENDING} con un intento posterior a su recepción. Si Wompi aprueba más
 * tarde, el webhook confirma normalmente sobre el pago que seguía
 * {@code PENDING}; si para entonces ya hubo un recobro exitoso sobre el mismo
 * documento, la segunda confirmación excede el saldo del documento y se
 * resuelve como saldo a favor por sobrepago, nunca como un cargo duplicado
 * silencioso.
 *
 * <p>
 * <strong>Sin {@code @Transactional}</strong>: consulta a Wompi por cada
 * candidato; la regla dura {@code SIN_IO_EXTERNO_EN_TRANSACCION} sigue la
 * cadena de llamadas hasta ahí.
 */
@Observed(name = "payment.gateway.reconciliation.run")
@Service
public class ReconcilePendingPaymentsService implements ReconcilePendingPaymentsUseCase {

    private static final Logger log = LoggerFactory
            .getLogger(ReconcilePendingPaymentsService.class);

    private final StalePendingPaymentQueryPort stalePendingPaymentQueryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort;
    private final GatewayOutcomeSettler outcomeSettler;
    private final PaymentAttemptRecorderPort paymentAttemptRecorderPort;
    private final PaymentAttemptQueryPort paymentAttemptQueryPort;
    private final ObservationRegistry observationRegistry;
    private final Clock clock;
    private final PaymentGatewayMetrics metrics;

    public ReconcilePendingPaymentsService(
            StalePendingPaymentQueryPort stalePendingPaymentQueryPort,
            PaymentGatewayPort paymentGatewayPort,
            SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort,
            GatewayOutcomeSettler outcomeSettler,
            PaymentAttemptRecorderPort paymentAttemptRecorderPort,
            PaymentAttemptQueryPort paymentAttemptQueryPort,
            ObservationRegistry observationRegistry, Clock clock, PaymentGatewayMetrics metrics) {
        this.stalePendingPaymentQueryPort = stalePendingPaymentQueryPort;
        this.paymentGatewayPort = paymentGatewayPort;
        this.subscriptionPaymentLedgerPort = subscriptionPaymentLedgerPort;
        this.outcomeSettler = outcomeSettler;
        this.paymentAttemptRecorderPort = paymentAttemptRecorderPort;
        this.paymentAttemptQueryPort = paymentAttemptQueryPort;
        this.observationRegistry = observationRegistry;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Override
    public ReconciliationBatchResult reconcileOlderThan(LocalDateTime cutoff, int limit) {
        List<StalePendingPayment> candidates = stalePendingPaymentQueryPort.findOlderThan(cutoff,
                limit);
        int processed = 0;
        int resolved = 0;
        int failures = 0;
        for (StalePendingPayment candidate : candidates) {
            try {
                if (reconcileOne(candidate)) {
                    resolved++;
                }
                processed++;
            } catch (WompiRateLimitedException e) {
                log.warn("Wompi limitó la tasa conciliando el pago {}: se detiene el lote,"
                        + " reintenta en {}", candidate.paymentId(), e.retryAfter());
                metrics.recordCollectionFailure(FailureKind.TRANSIENT);
                tagBatch(failures);
                return new ReconciliationBatchResult(processed, resolved, failures);
            } catch (WompiGatewayException e) {
                log.warn("Fallo transitorio de la pasarela conciliando el pago {}",
                        candidate.paymentId(), e);
                metrics.recordCollectionFailure(FailureKind.TRANSIENT);
                processed++;
                failures++;
            } catch (RuntimeException e) {
                log.error("Fallo determinista conciliando el pago {}", candidate.paymentId(), e);
                metrics.recordCollectionFailure(FailureKind.DETERMINISTIC);
                processed++;
                failures++;
            }
        }
        tagBatch(failures);
        return new ReconciliationBatchResult(processed, resolved, failures);
    }

    private boolean reconcileOne(StalePendingPayment candidate) {
        if (candidate.gatewayReference() == null) {
            return failOrphanedReservation(candidate);
        }
        GatewayTransaction transaction = paymentGatewayPort
                .findTransaction(candidate.gatewayReference());
        if (!transaction.status().isFinal()) {
            return failIfPendingTooLong(candidate);
        }
        return settleFinal(candidate, transaction);
    }

    /**
     * Antes de interpretar cualquier estado, la referencia queda asignada: si Wompi
     * tiene una transacción para este {@code client_request_id}, la reserva deja de
     * estar huérfana aunque siga {@code PENDING} en Wompi. Solo si Wompi tampoco la
     * conoce se falla directamente -la reserva ya superó el corte de
     * {@code reconcileOlderThan}, así que no hace falta otro umbral.
     */
    private boolean failOrphanedReservation(StalePendingPayment candidate) {
        Optional<GatewayTransaction> found = paymentGatewayPort
                .findByReference(candidate.clientRequestId());
        if (found.isEmpty()) {
            return failWithConfigurationAttempt(candidate);
        }
        GatewayTransaction transaction = found.get();
        LocalDateTime gatewayCreatedAt = transaction.createdAt() == null
                ? LocalDateTime.now(clock)
                : LocalDateTime.ofInstant(transaction.createdAt(), clock.getZone());
        subscriptionPaymentLedgerPort.assignGatewayReference(candidate.paymentId(),
                candidate.companyId(), transaction.id(), gatewayCreatedAt);
        if (!transaction.status().isFinal()) {
            return false;
        }
        return settleFinal(candidate, transaction);
    }

    /**
     * {@code documentId} nulo —la aplicación del pago se perdió antes de fallar— no
     * puede anotar un intento: {@code RecordPaymentAttemptService} exige un
     * documento existente y lanzaría después de que {@code fail} ya confirmó al
     * pago como fallido. Se deja constancia por log y métrica en vez de propagar
     * una excepción sobre un cambio de estado que ya es irreversible.
     */
    private boolean failWithConfigurationAttempt(StalePendingPayment candidate) {
        Long documentId = subscriptionPaymentLedgerPort
                .findDocumentIdByPayment(candidate.companyId(), candidate.paymentId()).orElse(null);
        subscriptionPaymentLedgerPort.fail(candidate.paymentId(), candidate.companyId());
        if (documentId == null) {
            log.warn("Pago {} fallado sin documento resuelto: no se anota intento de reintento",
                    candidate.paymentId());
            metrics.recordCollectionFailure(FailureKind.DETERMINISTIC);
            return true;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        paymentAttemptRecorderPort.record(candidate.companyId(), documentId, null,
                PaymentGatewayNames.WOMPI, candidate.amount(), null,
                GatewayDeclineKind.CONFIGURATION, now, now.plusDays(1));
        return true;
    }

    /**
     * Un {@code PENDING} que Wompi sostiene más allá de
     * {@code pending-transaction-max-age} bloquea el recobro sin que nadie lo
     * cierre nunca: se anota un intento {@code SOFT} para desatascar la escalera de
     * reintentos, pero el pago se mantiene {@code PENDING} —ver el javadoc de la
     * clase—. Si ya se anotó un intento tras la recepción del pago en una pasada
     * anterior, no se repite: sería un intento nuevo por cada barrido mientras
     * Wompi no resuelva la transacción.
     */
    private boolean failIfPendingTooLong(StalePendingPayment candidate) {
        if (candidate.receivedAt() == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (candidate.receivedAt()
                .isAfter(now.minus(paymentGatewayPort.pendingTransactionMaxAge()))) {
            return false;
        }
        Long documentId = subscriptionPaymentLedgerPort
                .findDocumentIdByPayment(candidate.companyId(), candidate.paymentId()).orElse(null);
        if (documentId != null
                && alreadyMarkedStale(candidate.companyId(), documentId, candidate.receivedAt())) {
            return false;
        }
        int priorAttempts = documentId == null
                ? 0
                : paymentAttemptQueryPort.countRetryableSince(candidate.companyId(), documentId,
                        now.minus(RetrySchedule.RETRY_WINDOW));
        paymentAttemptRecorderPort.record(candidate.companyId(), documentId, null,
                PaymentGatewayNames.WOMPI, candidate.amount(), null, GatewayDeclineKind.SOFT, now,
                RetrySchedule.nextAttemptAt(priorAttempts, now));
        return true;
    }

    private boolean alreadyMarkedStale(Long companyId, Long documentId, LocalDateTime receivedAt) {
        return paymentAttemptQueryPort.findLast(companyId, documentId)
                .map(attempt -> !attempt.attemptedAt().isBefore(receivedAt)).orElse(false);
    }

    /**
     * Compara el importe antes de confirmar (mismo criterio que el webhook): un
     * pago que no cuadra con lo cobrado no se da por aprobado a ciegas.
     */
    private boolean settleFinal(StalePendingPayment candidate, GatewayTransaction transaction) {
        if (transaction.amountInCents() != GatewayCharger.toAmountInCents(candidate.amount())) {
            return failAmountMismatch(candidate, transaction);
        }
        Long documentId = subscriptionPaymentLedgerPort
                .findDocumentIdByPayment(candidate.companyId(), candidate.paymentId()).orElse(null);
        outcomeSettler.settle(transaction.status(), transaction.statusMessage(),
                candidate.companyId(), candidate.paymentId(), documentId, null, candidate.amount());
        return true;
    }

    /**
     * Un importe que no cuadra con la pasarela no es un rechazo del cliente sino un
     * problema propio (moneda, redondeo, comisión mal restada), así que no se deja
     * {@code PENDING} para siempre bloqueando el recobro —el mismo cuadro de
     * {@link #failIfPendingTooLong} por otra puerta— sino que se falla con
     * {@link GatewayDeclineKind#CONFIGURATION} para revisión humana.
     */
    private boolean failAmountMismatch(StalePendingPayment candidate,
            GatewayTransaction transaction) {
        log.error("Importe distinto conciliando el pago {}: pasarela {} centavos, registrado {}",
                candidate.paymentId(), transaction.amountInCents(), candidate.amount());
        metrics.recordCollectionFailure(FailureKind.DETERMINISTIC);
        return failWithConfigurationAttempt(candidate);
    }

    private void tagBatch(int failures) {
        Observation current = observationRegistry.getCurrentObservation();
        if (current == null) {
            return;
        }
        current.lowCardinalityKeyValue("job.outcome", failures > 0 ? "partial_failure" : "success");
        current.highCardinalityKeyValue("batch.failures", String.valueOf(failures));
        if (failures > 0) {
            current.error(new ReconciliationBatchPartiallyFailedException(failures));
        }
    }

    private static final class ReconciliationBatchPartiallyFailedException
            extends
                RuntimeException {
        private ReconciliationBatchPartiallyFailedException(int failures) {
            super(failures + " candidato(s) fallaron en el lote de conciliación");
        }
    }
}
