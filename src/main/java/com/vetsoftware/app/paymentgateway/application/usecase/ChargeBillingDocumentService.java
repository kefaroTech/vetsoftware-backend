package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.command.ChargeBillingDocumentCommand;
import com.vetsoftware.app.paymentgateway.application.dto.DocumentChargeDto;
import com.vetsoftware.app.paymentgateway.application.dto.GatewayChargeResult;
import com.vetsoftware.app.paymentgateway.application.port.in.ChargeBillingDocumentUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.BillingDocumentChargeQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PendingPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.domain.BillingDocumentChargeSnapshot;
import com.vetsoftware.app.paymentgateway.domain.DocumentChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.LastPaymentAttempt;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNotConfiguredException;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import com.vetsoftware.app.paymentgateway.domain.RetrySchedule;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Cobra un documento de cobro ya emitido: la renovación recurrente y el
 * reintento de un rechazo anterior son el mismo caso, solo cambia qué lo
 * dispara ({@code RunPaymentCollectionUseCase}).
 *
 * <p>
 * <strong>Sin {@code @Transactional}</strong>: el tramo mecánico contra Wompi
 * vive en {@link GatewayCharger} (I/O de punta a punta); la regla dura
 * {@code SIN_IO_EXTERNO_EN_TRANSACCION} sigue la cadena de llamadas hasta ahí.
 *
 * <p>
 * <strong>El presupuesto de reintentos nunca lanza aquí.</strong> A diferencia
 * de {@code RecordPaymentAttemptUseCase} (que rechaza con
 * {@code RetryBudgetExhaustedException} cuando alguien intenta anotar un quinto
 * intento), este caso de uso lo comprueba <em>antes</em> de cobrar y devuelve
 * {@code SKIPPED_BUDGET}: es un barrido de plataforma, no una petición que
 * pueda fallar con 409.
 */
@Observed(name = "payment.gateway.charge.document")
@Service
public class ChargeBillingDocumentService implements ChargeBillingDocumentUseCase {

    private static final Duration CONFIGURATION_DEDUPE_WINDOW = Duration.ofHours(24);

    private final BillingDocumentChargeQueryPort documentQueryPort;
    private final PendingPaymentQueryPort pendingPaymentQueryPort;
    private final PaymentAttemptQueryPort paymentAttemptQueryPort;
    private final DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort;
    private final PaymentAttemptRecorderPort paymentAttemptRecorderPort;
    private final GatewayCharger gatewayCharger;
    private final Clock clock;

    public ChargeBillingDocumentService(BillingDocumentChargeQueryPort documentQueryPort,
            PendingPaymentQueryPort pendingPaymentQueryPort,
            PaymentAttemptQueryPort paymentAttemptQueryPort,
            DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort,
            PaymentAttemptRecorderPort paymentAttemptRecorderPort, GatewayCharger gatewayCharger,
            Clock clock) {
        this.documentQueryPort = documentQueryPort;
        this.pendingPaymentQueryPort = pendingPaymentQueryPort;
        this.paymentAttemptQueryPort = paymentAttemptQueryPort;
        this.defaultCardPaymentMethodQueryPort = defaultCardPaymentMethodQueryPort;
        this.paymentAttemptRecorderPort = paymentAttemptRecorderPort;
        this.gatewayCharger = gatewayCharger;
        this.clock = clock;
    }

    @Override
    public DocumentChargeDto execute(ChargeBillingDocumentCommand command) {
        BillingDocumentChargeSnapshot document = documentQueryPort
                .findByIdAndCompanyId(command.billingDocumentId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Billing document not found: " + command.billingDocumentId()));

        if (document.balanceAmount().signum() <= 0) {
            return skip(DocumentChargeOutcome.SKIPPED_NO_BALANCE);
        }
        if (pendingPaymentQueryPort.existsPendingPayment(command.companyId(),
                document.documentId())) {
            return skip(DocumentChargeOutcome.SKIPPED_PENDING_PAYMENT);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Optional<LastPaymentAttempt> last = paymentAttemptQueryPort.findLast(command.companyId(),
                document.documentId());
        if (last.isPresent()) {
            LastPaymentAttempt attempt = last.get();
            if (attempt.declineKind() == GatewayDeclineKind.HARD) {
                return skip(DocumentChargeOutcome.SKIPPED_HARD_DECLINE);
            }
            if (attempt.nextAttemptAt() != null && attempt.nextAttemptAt().isAfter(now)) {
                return skip(DocumentChargeOutcome.SKIPPED_NOT_DUE);
            }
        }

        int retryable = paymentAttemptQueryPort.countRetryableSince(command.companyId(),
                document.documentId(), now.minus(RetrySchedule.RETRY_WINDOW));
        if (retryable >= RetrySchedule.MAX_SOFT_ATTEMPTS) {
            return skip(DocumentChargeOutcome.SKIPPED_BUDGET);
        }

        PaymentMethodRef paymentMethod = defaultCardPaymentMethodQueryPort
                .findDefaultActiveCard(command.companyId(), PaymentGatewayNames.WOMPI).orElse(null);
        if (paymentMethod == null) {
            recordConfigurationAttemptUnlessRecent(command, document, last, now);
            return skip(DocumentChargeOutcome.NO_PAYMENT_METHOD);
        }

        int nextAttemptNumber = last.map(LastPaymentAttempt::attemptNumber).orElse(0) + 1;
        String reference = "VS-DOC-" + document.documentId() + "-A" + nextAttemptNumber;

        GatewayChargeResult result;
        try {
            result = gatewayCharger.charge(command.companyId(), document.documentId(),
                    paymentMethod, document.balanceAmount(), document.currency(), reference);
        } catch (PaymentGatewayNotConfiguredException e) {
            return skip(DocumentChargeOutcome.NOT_CONFIGURED);
        }

        DocumentChargeOutcome outcome = switch (result.outcome()) {
            case APPROVED -> DocumentChargeOutcome.APPROVED;
            case PENDING -> DocumentChargeOutcome.PENDING;
            case DECLINED -> DocumentChargeOutcome.DECLINED;
            default -> throw new IllegalStateException(
                    "GatewayCharger devolvió un desenlace inesperado: " + result.outcome());
        };
        return new DocumentChargeDto(outcome, result.gatewayReference(), result.declineReason());
    }

    /**
     * Evita una fila diaria idéntica: si el último intento ya es un
     * {@code CONFIGURATION} de las últimas 24 h, este barrido no anota otro.
     */
    private void recordConfigurationAttemptUnlessRecent(ChargeBillingDocumentCommand command,
            BillingDocumentChargeSnapshot document, Optional<LastPaymentAttempt> last,
            LocalDateTime now) {
        boolean recentConfigurationAttempt = last
                .filter(attempt -> attempt.declineKind() == GatewayDeclineKind.CONFIGURATION)
                .filter(attempt -> attempt.attemptedAt()
                        .isAfter(now.minus(CONFIGURATION_DEDUPE_WINDOW)))
                .isPresent();
        if (recentConfigurationAttempt) {
            return;
        }
        paymentAttemptRecorderPort.record(command.companyId(), document.documentId(), null,
                PaymentGatewayNames.WOMPI, document.balanceAmount(), null,
                GatewayDeclineKind.CONFIGURATION, now, null);
    }

    private static DocumentChargeDto skip(DocumentChargeOutcome outcome) {
        return new DocumentChargeDto(outcome, null, null);
    }
}
