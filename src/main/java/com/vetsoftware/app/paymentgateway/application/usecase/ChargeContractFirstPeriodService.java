package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.command.ChargeContractFirstPeriodCommand;
import com.vetsoftware.app.paymentgateway.application.dto.FirstPeriodChargeDto;
import com.vetsoftware.app.paymentgateway.application.port.in.ChargeContractFirstPeriodUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.BillingDocumentIssuerPort;
import com.vetsoftware.app.paymentgateway.application.port.out.CompanyBillingEmailQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.FirstPeriodPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayPort;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.domain.ChargeRequest;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransaction;
import com.vetsoftware.app.paymentgateway.domain.IssuedPeriodDocument;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Cobra el primer periodo de un contrato recién firmado.
 *
 * <p>
 * <strong>Sin {@code @Transactional}</strong>: habla con Wompi (creación de la
 * transacción y hasta {@code status-poll-attempts} sondeos), y la regla dura
 * {@code SIN_IO_EXTERNO_EN_TRANSACCION} sigue la cadena de llamadas hasta aquí.
 * La espera entre sondeos usa {@link Thread#sleep(long)} sobre una
 * {@link Duration} de configuración, interrumpible, y fuera de cualquier
 * transacción no hay lock que retener mientras dura.
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

    private static final Logger log = LoggerFactory
            .getLogger(ChargeContractFirstPeriodService.class);

    private static final String REFERENCE_PREFIX = "VS-";
    private static final String REFERENCE_SUFFIX = "-P1";

    private final FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort;
    private final BillingDocumentIssuerPort billingDocumentIssuerPort;
    private final DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort;
    private final CompanyBillingEmailQueryPort companyBillingEmailQueryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort;
    private final PaymentAttemptRecorderPort paymentAttemptRecorderPort;
    private final GatewayOutcomeSettler outcomeSettler;
    private final Clock clock;

    public ChargeContractFirstPeriodService(FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort,
            BillingDocumentIssuerPort billingDocumentIssuerPort,
            DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort,
            CompanyBillingEmailQueryPort companyBillingEmailQueryPort,
            PaymentGatewayPort paymentGatewayPort,
            SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort,
            PaymentAttemptRecorderPort paymentAttemptRecorderPort,
            GatewayOutcomeSettler outcomeSettler, Clock clock) {
        this.firstPeriodPaymentQueryPort = firstPeriodPaymentQueryPort;
        this.billingDocumentIssuerPort = billingDocumentIssuerPort;
        this.defaultCardPaymentMethodQueryPort = defaultCardPaymentMethodQueryPort;
        this.companyBillingEmailQueryPort = companyBillingEmailQueryPort;
        this.paymentGatewayPort = paymentGatewayPort;
        this.subscriptionPaymentLedgerPort = subscriptionPaymentLedgerPort;
        this.paymentAttemptRecorderPort = paymentAttemptRecorderPort;
        this.outcomeSettler = outcomeSettler;
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

        String fiscalEmail = companyBillingEmailQueryPort.findFiscalEmail(command.companyId())
                .orElseThrow(() -> new IllegalStateException(
                        "La empresa " + command.companyId() + " no tiene perfil fiscal vigente"));
        long amountInCents = toAmountInCents(document.totalAmount());

        GatewayTransaction transaction = paymentGatewayPort
                .charge(new ChargeRequest(Long.parseLong(paymentMethod.token()), amountInCents,
                        document.currency(), reference, fiscalEmail));

        Long paymentId = subscriptionPaymentLedgerPort.registerAndApply(command.companyId(),
                document.totalAmount(), document.currency(), transaction.id(),
                LocalDateTime.now(clock), reference, document.documentId());

        GatewayTransaction finalTransaction = pollUntilFinal(transaction);
        if (!finalTransaction.status().isFinal()) {
            return new FirstPeriodChargeDto(FirstPeriodChargeOutcome.PENDING, transaction.id(),
                    null);
        }

        FirstPeriodChargeOutcome outcome = outcomeSettler.settle(finalTransaction.status(),
                finalTransaction.statusMessage(), command.companyId(), paymentId,
                document.documentId(), paymentMethod.id(), document.totalAmount());
        return new FirstPeriodChargeDto(outcome, transaction.id(),
                outcome == FirstPeriodChargeOutcome.DECLINED
                        ? finalTransaction.statusMessage()
                        : null);
    }

    /**
     * COP no factura fracciones de peso: el total ya llega sin decimales en la
     * inmensa mayoría de los casos. {@code setScale} solo absorbe el ruido de
     * escala que puede dejar la aritmética de {@code subscriptionbilling} (p. ej.
     * {@code 45000.00}); nunca redondea un valor que de verdad tuviera centavos,
     * porque el documento de cobro no los modela.
     */
    private static long toAmountInCents(BigDecimal totalAmount) {
        return totalAmount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private FirstPeriodChargeDto translateExisting(FirstPeriodPaymentSnapshot snapshot) {
        FirstPeriodChargeOutcome outcome = switch (snapshot.status()) {
            case "CONFIRMED", "REFUNDED" -> FirstPeriodChargeOutcome.APPROVED;
            case "PENDING" -> FirstPeriodChargeOutcome.PENDING;
            default -> FirstPeriodChargeOutcome.DECLINED;
        };
        return new FirstPeriodChargeDto(outcome, snapshot.gatewayReference(), null);
    }

    /**
     * Sondea hasta {@code status-poll-attempts} veces, con
     * {@code status-poll-interval} entre cada una. Interrumpible: una interrupción
     * del hilo corta el sondeo y deja el estado en el último conocido (normalmente
     * {@code PENDING}), que el webhook cerrará después.
     */
    private GatewayTransaction pollUntilFinal(GatewayTransaction initial) {
        GatewayTransaction current = initial;
        int attempts = paymentGatewayPort.statusPollAttempts();
        Duration interval = paymentGatewayPort.statusPollInterval();
        for (int i = 0; i < attempts && !current.status().isFinal(); i++) {
            if (!sleep(interval)) {
                break;
            }
            current = paymentGatewayPort.findTransaction(current.id());
        }
        return current;
    }

    private static boolean sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sondeo de estado de transacción Wompi interrumpido");
            return false;
        }
    }
}
