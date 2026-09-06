package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.command.ChargeContractFirstPeriodCommand;
import com.vetsoftware.app.paymentgateway.application.dto.FirstPeriodChargeDto;
import com.vetsoftware.app.paymentgateway.application.dto.GatewayChargeResult;
import com.vetsoftware.app.paymentgateway.application.port.in.ChargeContractFirstPeriodUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.BillingDocumentIssuerPort;
import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.FirstPeriodPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.IssuedPeriodDocument;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * Cobra el primer periodo de un contrato recién firmado.
 *
 * <p>
 * <strong>Sin {@code @Transactional}</strong>: el tramo mecánico (correo,
 * transacción, sondeo, desenlace) vive en {@link GatewayCharger} y es I/O de
 * punta a punta; la regla dura {@code SIN_IO_EXTERNO_EN_TRANSACCION} sigue la
 * cadena de llamadas hasta ahí.
 *
 * <p>
 * <strong>Idempotente por la referencia del contrato</strong>
 * ({@code VS-<numero>-P1}), no por reintento de la transacción de Wompi: un
 * segundo disparo de {@code afterCommit} (o del propio webhook) encuentra el
 * pago ya registrado y traduce su estado en vez de volver a cobrar.
 */
@Observed(name = "payment.gateway.charge.first.period")
@Service
public class ChargeContractFirstPeriodService implements ChargeContractFirstPeriodUseCase {

    private static final String REFERENCE_PREFIX = "VS-";
    private static final String REFERENCE_SUFFIX = "-P1";

    private final FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort;
    private final BillingDocumentIssuerPort billingDocumentIssuerPort;
    private final DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort;
    private final PaymentAttemptRecorderPort paymentAttemptRecorderPort;
    private final GatewayCharger gatewayCharger;
    private final Clock clock;

    public ChargeContractFirstPeriodService(FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort,
            BillingDocumentIssuerPort billingDocumentIssuerPort,
            DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort,
            PaymentAttemptRecorderPort paymentAttemptRecorderPort, GatewayCharger gatewayCharger,
            Clock clock) {
        this.firstPeriodPaymentQueryPort = firstPeriodPaymentQueryPort;
        this.billingDocumentIssuerPort = billingDocumentIssuerPort;
        this.defaultCardPaymentMethodQueryPort = defaultCardPaymentMethodQueryPort;
        this.paymentAttemptRecorderPort = paymentAttemptRecorderPort;
        this.gatewayCharger = gatewayCharger;
        this.clock = clock;
    }

    @Override
    public FirstPeriodChargeDto execute(ChargeContractFirstPeriodCommand command) {
        String reference = REFERENCE_PREFIX + command.subscriptionNumber() + REFERENCE_SUFFIX;

        var existing = firstPeriodPaymentQueryPort.findByCompanyIdAndReference(command.companyId(),
                reference);
        if (existing.isPresent()) {
            return translateExisting(existing.get());
        }

        IssuedPeriodDocument document = billingDocumentIssuerPort.issue(command.companyId(),
                command.subscriptionId(), command.periodStart(), command.periodEnd());
        if (document.documentId() == null) {
            return new FirstPeriodChargeDto(FirstPeriodChargeOutcome.NOT_CONFIGURED, null,
                    "No hay cargos pendientes para el periodo");
        }

        PaymentMethodRef paymentMethod = defaultCardPaymentMethodQueryPort
                .findDefaultActiveCard(command.companyId(), PaymentGatewayNames.WOMPI).orElse(null);
        if (paymentMethod == null) {
            paymentAttemptRecorderPort.record(command.companyId(), document.documentId(), null,
                    PaymentGatewayNames.WOMPI, document.totalAmount(), null,
                    GatewayDeclineKind.CONFIGURATION, LocalDateTime.now(clock), null);
            return new FirstPeriodChargeDto(FirstPeriodChargeOutcome.NO_PAYMENT_METHOD, null,
                    "La empresa no tiene un medio de pago Wompi activo");
        }

        GatewayChargeResult result = gatewayCharger.charge(command.companyId(),
                document.documentId(), paymentMethod, document.totalAmount(), document.currency(),
                reference);
        return new FirstPeriodChargeDto(result.outcome(), result.gatewayReference(),
                result.declineReason());
    }

    private FirstPeriodChargeDto translateExisting(FirstPeriodPaymentSnapshot snapshot) {
        FirstPeriodChargeOutcome outcome = switch (snapshot.status()) {
            case "CONFIRMED", "REFUNDED" -> FirstPeriodChargeOutcome.APPROVED;
            case "PENDING" -> FirstPeriodChargeOutcome.PENDING;
            default -> FirstPeriodChargeOutcome.DECLINED;
        };
        return new FirstPeriodChargeDto(outcome, snapshot.gatewayReference(), null);
    }
}
