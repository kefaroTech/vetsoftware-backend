package com.vetsoftware.app.paymentgateway.infrastructure.observability;

import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricNames;
import com.vetsoftware.app.paymentattempt.infrastructure.persistence.PaymentAttemptJpaRepository;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.SubscriptionPaymentJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import org.springframework.stereotype.Component;

/**
 * Dos sondas de guardia de cobranza: pagos {@code PENDING} de pasarela
 * envejecidos y tamano de la cola de reintentos. Cada valor se cachea 60 s para
 * que aumentar la frecuencia de scrape de Prometheus no aumente la carga SQL —
 * mismo motivo que {@code BusinessGaugeMetrics}, resuelto aqui con un cache
 * perezoso en vez de un {@code @Scheduled} propio porque son dos consultas de
 * conteo, no un snapshot que varias series deban compartir.
 */
@Component
public class PaymentGatewayGaugeMetrics implements MeterBinder {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final Duration PENDING_AGE_THRESHOLD = Duration.ofHours(1);

    private final SubscriptionPaymentJpaRepository subscriptionPayments;
    private final PaymentAttemptJpaRepository paymentAttempts;
    private final Clock clock;

    private final AtomicReference<CachedValue> pendingAgedCache = new AtomicReference<>();
    private final AtomicReference<CachedValue> retryQueueCache = new AtomicReference<>();

    public PaymentGatewayGaugeMetrics(SubscriptionPaymentJpaRepository subscriptionPayments,
            PaymentAttemptJpaRepository paymentAttempts, Clock clock) {
        this.subscriptionPayments = subscriptionPayments;
        this.paymentAttempts = paymentAttempts;
        this.clock = clock;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(BusinessMetricNames.SUBSCRIPTION_PAYMENTS_PENDING_AGED, this,
                PaymentGatewayGaugeMetrics::pendingAgedCount).baseUnit("payments")
                .description("Pagos PENDING de pasarela con mas de una hora sin resolverse")
                .register(registry);
        Gauge.builder(BusinessMetricNames.PAYMENT_ATTEMPT_RETRY_QUEUE_SIZE, this,
                PaymentGatewayGaugeMetrics::retryQueueSize).baseUnit("attempts")
                .description("Intentos de cobro con reintento programado a futuro")
                .register(registry);
    }

    private double pendingAgedCount() {
        return cached(pendingAgedCache,
                () -> subscriptionPayments.countByStatusAndGatewayIsNotNullAndReceivedAtBefore(
                        SubscriptionPaymentStatus.PENDING,
                        LocalDateTime.now(clock).minus(PENDING_AGE_THRESHOLD)));
    }

    private double retryQueueSize() {
        return cached(retryQueueCache,
                () -> paymentAttempts.countByNextAttemptAtGreaterThan(LocalDateTime.now(clock)));
    }

    private double cached(AtomicReference<CachedValue> cache, LongSupplier query) {
        Instant now = clock.instant();
        CachedValue current = cache.get();
        if (current != null && Duration.between(current.at(), now).compareTo(CACHE_TTL) < 0) {
            return current.value();
        }
        long value = query.getAsLong();
        cache.set(new CachedValue(now, value));
        return value;
    }

    private record CachedValue(Instant at, long value) {
    }
}
