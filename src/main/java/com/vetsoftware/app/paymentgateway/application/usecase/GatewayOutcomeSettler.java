package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineCode;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.paymentgateway.domain.RetrySchedule;
import com.vetsoftware.app.paymentgateway.domain.WompiDeclineClassifier;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Aplica el desenlace <strong>final</strong> de una transacción de Wompi al
 * resto del circuito de cobro: confirma o falla el pago y, si falla, anota el
 * intento. Lo comparten {@code ChargeContractFirstPeriodService} (tras el
 * sondeo), {@code ChargeBillingDocumentService} (el cobro recurrente) y
 * {@code ProcessWompiEventService} (el webhook), que son los únicos lugares que
 * ven un estado terminal.
 */
@Component
public class GatewayOutcomeSettler {

    private final SubscriptionPaymentLedgerPort ledgerPort;
    private final PaymentAttemptRecorderPort attemptPort;
    private final PaymentAttemptQueryPort attemptQueryPort;
    private final Clock clock;

    public GatewayOutcomeSettler(SubscriptionPaymentLedgerPort ledgerPort,
            PaymentAttemptRecorderPort attemptPort, PaymentAttemptQueryPort attemptQueryPort,
            Clock clock) {
        this.ledgerPort = ledgerPort;
        this.attemptPort = attemptPort;
        this.attemptQueryPort = attemptQueryPort;
        this.clock = clock;
    }

    public FirstPeriodChargeOutcome settle(GatewayTransactionStatus status, String statusMessage,
            Long companyId, Long paymentId, Long documentId, Long paymentMethodId,
            BigDecimal requestedAmount) {
        if (status == GatewayTransactionStatus.APPROVED) {
            ledgerPort.confirm(paymentId, companyId);
            return FirstPeriodChargeOutcome.APPROVED;
        }
        ledgerPort.fail(paymentId, companyId);
        GatewayDeclineKind kind = WompiDeclineClassifier.classify(status, statusMessage);
        String code = GatewayDeclineCode.resolve(statusMessage, status);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime nextAttemptAt = nextAttemptAt(kind, companyId, documentId, now);
        attemptPort.record(companyId, documentId, paymentMethodId, PaymentGatewayNames.WOMPI,
                requestedAmount, code, kind, now, nextAttemptAt);
        return FirstPeriodChargeOutcome.DECLINED;
    }

    /**
     * Escalera de reintento (informe 05): 1, 2 y 4 días desde el rechazo anterior,
     * y sin siguiente al cuarto intento imputable. {@code documentId} nulo —el
     * webhook no siempre resuelve uno— se trata como primer rechazo: no hay
     * documento sobre el que contar reintentos previos.
     */
    private LocalDateTime nextAttemptAt(GatewayDeclineKind kind, Long companyId, Long documentId,
            LocalDateTime now) {
        if (kind != GatewayDeclineKind.SOFT) {
            return null;
        }
        int priorAttempts = documentId == null
                ? 0
                : attemptQueryPort.countRetryableSince(companyId, documentId,
                        now.minus(RetrySchedule.RETRY_WINDOW));
        return RetrySchedule.nextAttemptAt(priorAttempts, now);
    }
}
