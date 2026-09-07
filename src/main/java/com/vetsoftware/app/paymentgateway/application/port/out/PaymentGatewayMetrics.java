package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.GatewayWebhookOutcome;

/** Contadores de negocio de la cadena de cobro Wompi. */
public interface PaymentGatewayMetrics {

    void recordWebhookOutcome(GatewayWebhookOutcome outcome);

    /**
     * Un intento real contra Wompi (no una omisión) terminó con este desenlace.
     * {@code declineKind} es {@code null} salvo cuando {@code outcome} es
     * {@link FirstPeriodChargeOutcome#DECLINED}.
     */
    void recordChargeOutcome(FirstPeriodChargeOutcome outcome, GatewayDeclineKind declineKind);

    /**
     * El límite de creación de payment-sources no pudo consultarse en Valkey y la
     * petición se dejó pasar sin consumir cupo (fail-open).
     */
    void recordRateLimitFailOpen();

    /**
     * Un candidato del barrido de cobranza o de conciliación falló, clasificado por
     * la rama del {@code catch} que lo atrapó.
     */
    void recordCollectionFailure(FailureKind kind);

    /**
     * Las tres poblaciones que hoy separan los {@code catch} de
     * {@code RunPaymentCollectionService} y
     * {@code ReconcilePendingPaymentsService}, en el mismo vocabulario que ya usan
     * sus mensajes de log.
     */
    enum FailureKind {

        TRANSIENT("transient"),

        DETERMINISTIC("deterministic"),

        BUDGET_EXHAUSTED("budget_exhausted");

        private final String value;

        FailureKind(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
