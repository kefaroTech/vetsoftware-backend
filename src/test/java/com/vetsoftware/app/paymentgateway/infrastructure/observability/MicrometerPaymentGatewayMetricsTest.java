package com.vetsoftware.app.paymentgateway.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricCardinalityFilter;
import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricNames;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics.FailureKind;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.GatewayWebhookOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MicrometerPaymentGatewayMetrics")
class MicrometerPaymentGatewayMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private MicrometerPaymentGatewayMetrics metrics() {
        registry.config().meterFilter(new BusinessMetricCardinalityFilter());
        return new MicrometerPaymentGatewayMetrics(registry);
    }

    @Test
    @DisplayName("cuenta un desenlace de webhook con el nombre del enum en minusculas")
    void cuenta_desenlace_de_webhook() {
        metrics().recordWebhookOutcome(GatewayWebhookOutcome.REJECTED_CHECKSUM);

        assertThat(registry.get(BusinessMetricNames.PAYMENT_GATEWAY_WEBHOOK_EVENTS)
                .tag("outcome", "rejected_checksum").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("un cobro aprobado no lleva causa de rechazo: decline.kind=none")
    void aprobado_sin_causa_de_rechazo() {
        metrics().recordChargeOutcome(FirstPeriodChargeOutcome.APPROVED, null);

        assertThat(registry.get(BusinessMetricNames.PAYMENT_GATEWAY_CHARGE_OUTCOMES)
                .tag("payment.outcome", "approved").tag("decline.kind", "none").counter().count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("un rechazo cuenta con su causa")
    void rechazo_cuenta_con_su_causa() {
        metrics().recordChargeOutcome(FirstPeriodChargeOutcome.DECLINED, GatewayDeclineKind.HARD);

        assertThat(registry.get(BusinessMetricNames.PAYMENT_GATEWAY_CHARGE_OUTCOMES)
                .tag("payment.outcome", "declined").tag("decline.kind", "hard").counter().count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("un evento fuera de ventana de frescura cuenta rejected_stale, distinto de rejected_checksum")
    void cuenta_desenlace_stale() {
        metrics().recordWebhookOutcome(GatewayWebhookOutcome.REJECTED_STALE);

        assertThat(registry.get(BusinessMetricNames.PAYMENT_GATEWAY_WEBHOOK_EVENTS)
                .tag("outcome", "rejected_stale").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("el fail-open del limite de payment-sources cuenta fuera del prefijo de negocio (SEC2-12)")
    void cuenta_fail_open_del_limite() {
        metrics().recordRateLimitFailOpen();

        assertThat(
                registry.get("vetsoftware.payment.gateway.rate.limit.fail.open").counter().count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("un fallo transitorio de la pasarela cuenta con su causa")
    void cuenta_fallo_transitorio_de_cobranza() {
        metrics().recordCollectionFailure(FailureKind.TRANSIENT);

        assertThat(registry.get(BusinessMetricNames.PAYMENT_GATEWAY_COLLECTION_FAILURES)
                .tag("failure.kind", "transient").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("un fallo determinista cuenta con su causa")
    void cuenta_fallo_determinista_de_cobranza() {
        metrics().recordCollectionFailure(FailureKind.DETERMINISTIC);

        assertThat(registry.get(BusinessMetricNames.PAYMENT_GATEWAY_COLLECTION_FAILURES)
                .tag("failure.kind", "deterministic").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("un presupuesto de reintentos agotado cuenta con su causa")
    void cuenta_presupuesto_agotado_de_cobranza() {
        metrics().recordCollectionFailure(FailureKind.BUDGET_EXHAUSTED);

        assertThat(registry.get(BusinessMetricNames.PAYMENT_GATEWAY_COLLECTION_FAILURES)
                .tag("failure.kind", "budget_exhausted").counter().count()).isEqualTo(1);
    }
}
