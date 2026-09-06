package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.command.ChargeBillingDocumentCommand;
import com.vetsoftware.app.paymentgateway.application.dto.DocumentChargeDto;
import com.vetsoftware.app.paymentgateway.application.dto.PaymentCollectionBatchResult;
import com.vetsoftware.app.paymentgateway.application.port.in.ChargeBillingDocumentUseCase;
import com.vetsoftware.app.paymentgateway.application.port.in.RunPaymentCollectionUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.DueRetryQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.NewRecurringChargeQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptReschedulerPort;
import com.vetsoftware.app.paymentgateway.domain.DocumentChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.DueRetryTarget;
import com.vetsoftware.app.paymentgateway.domain.RecurringChargeTarget;
import com.vetsoftware.app.shared.pagination.PageResult;
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
     */
    private static final Set<DocumentChargeOutcome> REPROGRAMABLES = EnumSet.of(
            DocumentChargeOutcome.SKIPPED_PENDING_PAYMENT, DocumentChargeOutcome.SKIPPED_BUDGET,
            DocumentChargeOutcome.SKIPPED_NOT_DUE, DocumentChargeOutcome.NO_PAYMENT_METHOD,
            DocumentChargeOutcome.NOT_CONFIGURED);

    private static final Logger log = LoggerFactory.getLogger(RunPaymentCollectionService.class);

    private final NewRecurringChargeQueryPort newChargeQueryPort;
    private final DueRetryQueryPort dueRetryQueryPort;
    private final PaymentAttemptReschedulerPort reschedulerPort;
    private final ChargeBillingDocumentUseCase chargeUseCase;

    public RunPaymentCollectionService(NewRecurringChargeQueryPort newChargeQueryPort,
            DueRetryQueryPort dueRetryQueryPort, PaymentAttemptReschedulerPort reschedulerPort,
            ChargeBillingDocumentUseCase chargeUseCase) {
        this.newChargeQueryPort = newChargeQueryPort;
        this.dueRetryQueryPort = dueRetryQueryPort;
        this.reschedulerPort = reschedulerPort;
        this.chargeUseCase = chargeUseCase;
    }

    @Override
    public PaymentCollectionBatchResult collectNewChargesAfter(long afterId, int batchSize) {
        List<RecurringChargeTarget> targets = newChargeQueryPort.findAfter(afterId, batchSize);
        Map<DocumentChargeOutcome, Integer> outcomeCounts = new EnumMap<>(
                DocumentChargeOutcome.class);
        int failures = 0;
        long lastId = afterId;
        for (RecurringChargeTarget target : targets) {
            lastId = Math.max(lastId, target.billingDocumentId());
            try {
                DocumentChargeDto result = chargeUseCase.execute(new ChargeBillingDocumentCommand(
                        target.companyId(), target.billingDocumentId()));
                tally(outcomeCounts, result.outcome());
            } catch (RuntimeException e) {
                log.warn("Fallo cobrando el documento nuevo {}: {}", target.billingDocumentId(),
                        e.getMessage());
                failures++;
            }
        }
        return new PaymentCollectionBatchResult(targets.size(), failures, lastId, outcomeCounts);
    }

    @Override
    public PaymentCollectionBatchResult collectDueRetries(LocalDateTime now, int page,
            int pageSize) {
        PageResult<DueRetryTarget> due = dueRetryQueryPort.listDue(now, page, pageSize);
        Map<DocumentChargeOutcome, Integer> outcomeCounts = new EnumMap<>(
                DocumentChargeOutcome.class);
        int failures = 0;
        for (DueRetryTarget target : due.content()) {
            try {
                DocumentChargeDto result = chargeUseCase.execute(new ChargeBillingDocumentCommand(
                        target.companyId(), target.billingDocumentId()));
                tally(outcomeCounts, result.outcome());
                if (REPROGRAMABLES.contains(result.outcome())) {
                    reschedulerPort.reschedule(target.attemptId(), target.companyId(),
                            now.plusDays(1));
                }
            } catch (RuntimeException e) {
                log.warn("Fallo reintentando el intento {}: {}", target.attemptId(),
                        e.getMessage());
                failures++;
            }
        }
        return new PaymentCollectionBatchResult(due.content().size(), failures, 0L, outcomeCounts);
    }

    private static void tally(Map<DocumentChargeOutcome, Integer> counts,
            DocumentChargeOutcome outcome) {
        counts.merge(outcome, 1, Integer::sum);
    }
}
