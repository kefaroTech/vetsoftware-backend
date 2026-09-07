package com.vetsoftware.app.paymentgateway.infrastructure.observability;

import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricNames;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.GatewayWebhookOutcome;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class MicrometerPaymentGatewayMetrics implements PaymentGatewayMetrics {

    private static final String NONE_DECLINE_KIND = "none";

    /**
     * Fuera de {@code vetsoftware.business.*} a propósito: sin etiquetas y con una
     * sola serie, no necesita pasar por la lista blanca de cardinalidad de
     * {@code BusinessMetricCardinalityFilter} ni por sus paneles/alertas.
     */
    private static final String RATE_LIMIT_FAIL_OPEN = "vetsoftware.payment.gateway.rate.limit.fail.open";

    private final MeterRegistry registry;

    public MicrometerPaymentGatewayMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordWebhookOutcome(GatewayWebhookOutcome outcome) {
        Counter.builder(BusinessMetricNames.PAYMENT_GATEWAY_WEBHOOK_EVENTS)
                .description("Webhooks de Wompi procesados, por desenlace")
                .tag("outcome", lower(outcome.name())).register(registry).increment();
    }

    @Override
    public void recordChargeOutcome(FirstPeriodChargeOutcome outcome,
            GatewayDeclineKind declineKind) {
        Counter.builder(BusinessMetricNames.PAYMENT_GATEWAY_CHARGE_OUTCOMES).description(
                "Intentos reales de cobro contra Wompi resueltos, por desenlace y causa de rechazo")
                .tag("payment.outcome", lower(outcome.name()))
                .tag("decline.kind",
                        declineKind == null ? NONE_DECLINE_KIND : lower(declineKind.name()))
                .register(registry).increment();
    }

    @Override
    public void recordRateLimitFailOpen() {
        Counter.builder(RATE_LIMIT_FAIL_OPEN).description(
                "El límite de payment-sources no pudo consultarse en Valkey; la petición se dejó pasar")
                .register(registry).increment();
    }

    @Override
    public void recordCollectionFailure(FailureKind kind) {
        Counter.builder(BusinessMetricNames.PAYMENT_GATEWAY_COLLECTION_FAILURES)
                .description("Candidatos de cobranza o conciliacion que fallaron, por causa")
                .tag("failure.kind", kind.value()).register(registry).increment();
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
