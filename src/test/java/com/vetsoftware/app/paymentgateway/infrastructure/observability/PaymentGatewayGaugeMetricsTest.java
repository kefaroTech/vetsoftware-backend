package com.vetsoftware.app.paymentgateway.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricNames;
import com.vetsoftware.app.paymentattempt.infrastructure.persistence.PaymentAttemptJpaRepository;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.SubscriptionPaymentJpaRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentGatewayGaugeMetrics")
class PaymentGatewayGaugeMetricsTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-04T12:00:00Z"),
            ZoneOffset.UTC);

    @Test
    @DisplayName("publica el conteo de pagos PENDING envejecidos y de la cola de reintentos")
    void publica_los_dos_conteos() {
        SubscriptionPaymentJpaRepository subscriptionPayments = mock(
                SubscriptionPaymentJpaRepository.class);
        PaymentAttemptJpaRepository paymentAttempts = mock(PaymentAttemptJpaRepository.class);
        when(subscriptionPayments.countByStatusAndGatewayIsNotNullAndReceivedAtBefore(
                any(SubscriptionPaymentStatus.class), any(LocalDateTime.class))).thenReturn(3L);
        when(paymentAttempts.countByNextAttemptAtGreaterThan(any(LocalDateTime.class)))
                .thenReturn(7L);

        PaymentGatewayGaugeMetrics gauges = new PaymentGatewayGaugeMetrics(subscriptionPayments,
                paymentAttempts, RELOJ);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        gauges.bindTo(registry);

        assertThat(registry.get(BusinessMetricNames.SUBSCRIPTION_PAYMENTS_PENDING_AGED).gauge()
                .value()).isEqualTo(3);
        assertThat(
                registry.get(BusinessMetricNames.PAYMENT_ATTEMPT_RETRY_QUEUE_SIZE).gauge().value())
                .isEqualTo(7);
    }

    @Test
    @DisplayName("no repite la consulta a la base de datos dentro de la ventana de cache")
    void no_repite_la_consulta_dentro_de_la_ventana_de_cache() {
        SubscriptionPaymentJpaRepository subscriptionPayments = mock(
                SubscriptionPaymentJpaRepository.class);
        PaymentAttemptJpaRepository paymentAttempts = mock(PaymentAttemptJpaRepository.class);
        when(subscriptionPayments.countByStatusAndGatewayIsNotNullAndReceivedAtBefore(
                any(SubscriptionPaymentStatus.class), any(LocalDateTime.class))).thenReturn(1L);

        PaymentGatewayGaugeMetrics gauges = new PaymentGatewayGaugeMetrics(subscriptionPayments,
                paymentAttempts, RELOJ);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        gauges.bindTo(registry);

        registry.get(BusinessMetricNames.SUBSCRIPTION_PAYMENTS_PENDING_AGED).gauge().value();
        registry.get(BusinessMetricNames.SUBSCRIPTION_PAYMENTS_PENDING_AGED).gauge().value();

        verify(subscriptionPayments, times(1)).countByStatusAndGatewayIsNotNullAndReceivedAtBefore(
                any(SubscriptionPaymentStatus.class), any(LocalDateTime.class));
    }
}
