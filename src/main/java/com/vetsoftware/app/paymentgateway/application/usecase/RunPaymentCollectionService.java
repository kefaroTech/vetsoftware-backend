package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.command.ChargeBillingDocumentCommand;
import com.vetsoftware.app.paymentgateway.application.dto.DocumentChargeDto;
import com.vetsoftware.app.paymentgateway.application.dto.PaymentCollectionBatchResult;
import com.vetsoftware.app.paymentgateway.application.port.in.ChargeBillingDocumentUseCase;
import com.vetsoftware.app.paymentgateway.application.port.in.RunPaymentCollectionUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.DueRetryQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.NewRecurringChargeQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptReschedulerPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics.FailureKind;
import com.vetsoftware.app.paymentattempt.domain.RetryBudgetExhaustedException;
import com.vetsoftware.app.paymentgateway.domain.DocumentChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.DueRetryTarget;
import com.vetsoftware.app.paymentgateway.domain.RecurringChargeTarget;
import com.vetsoftware.app.paymentgateway.domain.WompiGatewayException;
import com.vetsoftware.app.paymentgateway.domain.WompiRateLimitedException;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * El trabajo de un lote del barrido diario de cobro: documentos nuevos por un
 * lado, reintentos vencidos por otro. {@code PaymentCollectionJob} hace las dos
 * vueltas de cursor/página; aquí vive la decisión de negocio de cada una.
 */
@Observed(name = "payment.gateway.collection.run")
@Service
public class RunPaymentCollectionService implements RunPaymentCollectionUseCase {

    /**
     * Solo se reprograma el intento vencido cuando el cobro no dejo rastro propio:
     * un rechazo registra un intento nuevo con su propia fecha, un cobro aprobado o
     * pendiente ya no debe volver a la cola, y un documento sin saldo o con rechazo
     * duro deja de salir por la propia consulta.
     *
     * <p>
     * {@code SKIPPED_BUDGET} queda fuera a proposito: reprogramarlo a +1 dia hace
     * que, al deslizar la ventana de 14 dias, el documento vuelva a cobrarse cada
     * vez que baje de cuatro intentos retenibles -un quinto, sexto... cobro
     * automatico que la escalera de reintentos ya dijo que no habria.
     */
    private static final Set<DocumentChargeOutcome> REPROGRAMABLES = EnumSet.of(
            DocumentChargeOutcome.SKIPPED_PENDING_PAYMENT, DocumentChargeOutcome.SKIPPED_NOT_DUE,
            DocumentChargeOutcome.NO_PAYMENT_METHOD, DocumentChargeOutcome.NOT_CONFIGURED);

    private static final Logger log = LoggerFactory.getLogger(RunPaymentCollectionService.class);

    private final NewRecurringChargeQueryPort newChargeQueryPort;
    private final DueRetryQueryPort dueRetryQueryPort;
    private final PaymentAttemptReschedulerPort reschedulerPort;
    private final ChargeBillingDocumentUseCase chargeUseCase;
    private final ObservationRegistry observationRegistry;
    private final PaymentGatewayMetrics metrics;

    public RunPaymentCollectionService(NewRecurringChargeQueryPort newChargeQueryPort,
            DueRetryQueryPort dueRetryQueryPort, PaymentAttemptReschedulerPort reschedulerPort,
            ChargeBillingDocumentUseCase chargeUseCase, ObservationRegistry observationRegistry,
            PaymentGatewayMetrics metrics) {
        this.newChargeQueryPort = newChargeQueryPort;
        this.dueRetryQueryPort = dueRetryQueryPort;
        this.reschedulerPort = reschedulerPort;
        this.chargeUseCase = chargeUseCase;
        this.observationRegistry = observationRegistry;
        this.metrics = metrics;
    }

    @Override
    public PaymentCollectionBatchResult collectNewChargesAfter(long afterId, int batchSize) {
        List<RecurringChargeTarget> targets = newChargeQueryPort.findAfter(afterId, batchSize);
        Map<DocumentChargeOutcome, Integer> outcomeCounts = new EnumMap<>(
                DocumentChargeOutcome.class);
        int failures = 0;
        int processed = 0;
        long lastId = afterId;
        for (RecurringChargeTarget target : targets) {
            lastId = Math.max(lastId, target.billingDocumentId());
            try {
                DocumentChargeDto result = chargeUseCase.execute(new ChargeBillingDocumentCommand(
                        target.companyId(), target.billingDocumentId()));
                tally(outcomeCounts, result.outcome());
                processed++;
            } catch (WompiRateLimitedException e) {
                log.warn("Wompi limitó la tasa cobrando el documento nuevo {}: se detiene el lote,"
                        + " reintenta en {}", target.billingDocumentId(), e.retryAfter());
                metrics.recordCollectionFailure(FailureKind.TRANSIENT);
                tagBatch(failures);
                return new PaymentCollectionBatchResult(processed, failures, lastId, outcomeCounts);
            } catch (WompiGatewayException e) {
                log.warn("Fallo transitorio de la pasarela cobrando el documento nuevo {}",
                        target.billingDocumentId(), e);
                metrics.recordCollectionFailure(FailureKind.TRANSIENT);
                processed++;
                failures++;
            } catch (RetryBudgetExhaustedException e) {
                log.info("Presupuesto de reintentos agotado cobrando el documento nuevo {}",
                        target.billingDocumentId(), e);
                metrics.recordCollectionFailure(FailureKind.BUDGET_EXHAUSTED);
                processed++;
                failures++;
            } catch (RuntimeException e) {
                log.error("Fallo determinista cobrando el documento nuevo {}",
                        target.billingDocumentId(), e);
                metrics.recordCollectionFailure(FailureKind.DETERMINISTIC);
                processed++;
                failures++;
            }
        }
        tagBatch(failures);
        return new PaymentCollectionBatchResult(processed, failures, lastId, outcomeCounts);
    }

    @Override
    public PaymentCollectionBatchResult collectDueRetries(LocalDateTime now, int page,
            int pageSize) {
        PageResult<DueRetryTarget> due = dueRetryQueryPort.listDue(now, page, pageSize);
        Map<DocumentChargeOutcome, Integer> outcomeCounts = new EnumMap<>(
                DocumentChargeOutcome.class);
        int failures = 0;
        int processed = 0;
        for (DueRetryTarget target : due.content()) {
            try {
                DocumentChargeDto result = chargeUseCase.execute(new ChargeBillingDocumentCommand(
                        target.companyId(), target.billingDocumentId()));
                tally(outcomeCounts, result.outcome());
                if (REPROGRAMABLES.contains(result.outcome())) {
                    reschedulerPort.reschedule(target.attemptId(), target.companyId(),
                            now.plusDays(1));
                }
                processed++;
            } catch (WompiRateLimitedException e) {
                log.warn(
                        "Wompi limitó la tasa reintentando el intento {}: se detiene el lote,"
                                + " reprogramado a {}",
                        target.attemptId(), now.plus(e.retryAfter()));
                reschedulerPort.reschedule(target.attemptId(), target.companyId(),
                        now.plus(e.retryAfter()));
                metrics.recordCollectionFailure(FailureKind.TRANSIENT);
                tagBatch(failures);
                return new PaymentCollectionBatchResult(processed, failures, 0L, outcomeCounts);
            } catch (WompiGatewayException e) {
                log.warn("Fallo transitorio de la pasarela reintentando el intento {}",
                        target.attemptId(), e);
                metrics.recordCollectionFailure(FailureKind.TRANSIENT);
                processed++;
                failures++;
            } catch (RetryBudgetExhaustedException e) {
                log.info("Presupuesto de reintentos agotado reintentando el intento {}",
                        target.attemptId(), e);
                metrics.recordCollectionFailure(FailureKind.BUDGET_EXHAUSTED);
                processed++;
                failures++;
            } catch (RuntimeException e) {
                log.error("Fallo determinista reintentando el intento {}", target.attemptId(), e);
                metrics.recordCollectionFailure(FailureKind.DETERMINISTIC);
                processed++;
                failures++;
            }
        }
        tagBatch(failures);
        return new PaymentCollectionBatchResult(processed, failures, 0L, outcomeCounts);
    }

    private static void tally(Map<DocumentChargeOutcome, Integer> counts,
            DocumentChargeOutcome outcome) {
        counts.merge(outcome, 1, Integer::sum);
    }

    private void tagBatch(int failures) {
        Observation current = observationRegistry.getCurrentObservation();
        if (current == null) {
            return;
        }
        current.lowCardinalityKeyValue("job.outcome", failures > 0 ? "partial_failure" : "success");
        current.highCardinalityKeyValue("batch.failures", String.valueOf(failures));
        if (failures > 0) {
            current.error(new CollectionBatchPartiallyFailedException(failures));
        }
    }

    private static final class CollectionBatchPartiallyFailedException extends RuntimeException {
        private CollectionBatchPartiallyFailedException(int failures) {
            super(failures + " candidato(s) fallaron en el lote de cobro");
        }
    }
}
