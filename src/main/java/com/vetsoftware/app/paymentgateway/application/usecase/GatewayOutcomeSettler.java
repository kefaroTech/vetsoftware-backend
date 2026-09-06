package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineCode;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.paymentgateway.domain.WompiDeclineClassifier;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Aplica el desenlace <strong>final</strong> de una transacción de Wompi al
 * resto del circuito de cobro: confirma o falla el pago y, si falla, anota el
 * intento. Lo comparten {@code ChargeContractFirstPeriodService} (tras el
 * sondeo) y {@code ProcessWompiEventService} (el webhook), que son los dos
 * únicos lugares que ven un estado terminal.
 */
@Component
public class GatewayOutcomeSettler {

    private final SubscriptionPaymentLedgerPort ledgerPort;
    private final PaymentAttemptRecorderPort attemptPort;
    private final Clock clock;

    public GatewayOutcomeSettler(SubscriptionPaymentLedgerPort ledgerPort,
            PaymentAttemptRecorderPort attemptPort, Clock clock) {
        this.ledgerPort = ledgerPort;
        this.attemptPort = attemptPort;
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
        LocalDateTime nextAttemptAt = kind == GatewayDeclineKind.SOFT ? now.plusDays(1) : null;
        attemptPort.record(companyId, documentId, paymentMethodId, PaymentGatewayNames.WOMPI,
                requestedAmount, code, kind, now, nextAttemptAt);
        return FirstPeriodChargeOutcome.DECLINED;
    }
}
