package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.dto.GatewayChargeResult;
import com.vetsoftware.app.paymentgateway.application.port.out.CompanyBillingEmailQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayPort;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.domain.ChargeRequest;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.FiscalProfileNotConfiguredException;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransaction;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import com.vetsoftware.app.paymentgateway.domain.PaymentReservation;
import com.vetsoftware.app.paymentgateway.domain.PaymentReservationOutcome;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * El tramo mecánico que comparte todo cobro contra Wompi con la fuente de pago
 * ya resuelta: correo fiscal, transacción, pago PENDING aplicado, sondeo y
 * desenlace final. Lo usan {@code ChargeContractFirstPeriodService} (primer
 * periodo) y {@code ChargeBillingDocumentService} (recurrente); ninguno de los
 * dos vuelve a sondear por su cuenta.
 *
 * <p>
 * <strong>Sin {@code @Transactional}</strong>: es I/O contra Wompi de punta a
 * punta, incluida la espera entre sondeos.
 *
 * <p>
 * <strong>No resuelve el medio de pago.</strong> Decidir
 * {@code NO_PAYMENT_METHOD} diverge entre los dos llamadores —el cobro
 * recurrente no repite un intento {@code CONFIGURATION} si ya anotó uno en las
 * últimas 24 h—, así que esa parte la resuelve cada llamador antes de entrar
 * aquí.
 */
@Component
public class GatewayCharger {

    private static final Logger log = LoggerFactory.getLogger(GatewayCharger.class);

    private final CompanyBillingEmailQueryPort companyBillingEmailQueryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort;
    private final GatewayOutcomeSettler outcomeSettler;
    private final PaymentGatewayMetrics metrics;
    private final Clock clock;

    public GatewayCharger(CompanyBillingEmailQueryPort companyBillingEmailQueryPort,
            PaymentGatewayPort paymentGatewayPort,
            SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort,
            GatewayOutcomeSettler outcomeSettler, PaymentGatewayMetrics metrics, Clock clock) {
        this.companyBillingEmailQueryPort = companyBillingEmailQueryPort;
        this.paymentGatewayPort = paymentGatewayPort;
        this.subscriptionPaymentLedgerPort = subscriptionPaymentLedgerPort;
        this.outcomeSettler = outcomeSettler;
        this.metrics = metrics;
        this.clock = clock;
    }

    public GatewayChargeResult charge(Long companyId, Long billingDocumentId,
            PaymentMethodRef paymentMethod, BigDecimal amount, String currency, String reference) {
        Optional<PaymentReservation> existing = subscriptionPaymentLedgerPort
                .findByClientRequestId(companyId, reference);
        if (existing.isPresent()) {
            return alreadyReserved(companyId, existing.get());
        }

        String fiscalEmail = companyBillingEmailQueryPort.findFiscalEmail(companyId)
                .orElseThrow(() -> new FiscalProfileNotConfiguredException(companyId));
        long amountInCents = toAmountInCents(amount);

        // Reserva antes del POST /transactions: si el proceso muere entre la
        // respuesta de Wompi y el registro, la conciliacion encuentra la
        // reserva en vez de un cobro sin rastro.
        PaymentReservationOutcome reservation = subscriptionPaymentLedgerPort.registerAndApply(
                companyId, amount, currency, null, LocalDateTime.now(clock), reference,
                billingDocumentId);
        if (reservation.recovered()) {
            PaymentReservation winner = subscriptionPaymentLedgerPort
                    .findByClientRequestId(companyId, reference)
                    .orElseThrow(() -> new IllegalStateException(
                            "Reserva recuperada sin fila persistida: " + reference));
            return alreadyReserved(companyId, winner);
        }
        Long paymentId = reservation.paymentId();

        GatewayTransaction transaction = paymentGatewayPort
                .charge(new ChargeRequest(Long.parseLong(paymentMethod.token()), amountInCents,
                        currency, reference, fiscalEmail));

        subscriptionPaymentLedgerPort.assignGatewayReference(paymentId, companyId, transaction.id(),
                gatewayCreatedAt(transaction));

        GatewayTransaction finalTransaction = pollUntilFinal(transaction);
        if (!finalTransaction.status().isFinal()) {
            metrics.recordChargeOutcome(FirstPeriodChargeOutcome.PENDING, null);
            return new GatewayChargeResult(FirstPeriodChargeOutcome.PENDING, transaction.id(),
                    null);
        }

        FirstPeriodChargeOutcome outcome = outcomeSettler.settle(finalTransaction.status(),
                finalTransaction.statusMessage(), companyId, paymentId, billingDocumentId,
                paymentMethod.id(), amount);
        String declineReason = outcome == FirstPeriodChargeOutcome.DECLINED
                ? finalTransaction.statusMessage()
                : null;
        return new GatewayChargeResult(outcome, transaction.id(), declineReason);
    }

    /**
     * Otra llamada ya reservó o cobró esta referencia (R13): no se vuelve a cargar
     * la misma tarjeta. Si ya hay un desenlace final registrado se devuelve ese;
     * mientras siga en vuelo se informa {@code PENDING} con la referencia que ya
     * existe.
     */
    private GatewayChargeResult alreadyReserved(Long companyId, PaymentReservation reservation) {
        FirstPeriodChargeOutcome outcome = subscriptionPaymentLedgerPort
                .currentOutcome(reservation.paymentId(), companyId)
                .orElse(FirstPeriodChargeOutcome.PENDING);
        return new GatewayChargeResult(outcome, reservation.gatewayReference(), null);
    }

    private LocalDateTime gatewayCreatedAt(GatewayTransaction transaction) {
        return transaction.createdAt() == null
                ? LocalDateTime.now(clock)
                : LocalDateTime.ofInstant(transaction.createdAt(), clock.getZone());
    }

    /**
     * COP no factura fracciones de peso: el total ya llega sin decimales en la
     * inmensa mayoría de los casos. {@code setScale} solo absorbe el ruido de
     * escala que puede dejar la aritmética de {@code subscriptionbilling} (p. ej.
     * {@code 45000.00}); nunca redondea un valor que de verdad tuviera centavos.
     */
    static long toAmountInCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
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
