package com.vetsoftware.app.paymentgateway.infrastructure.gateway;

import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentSourceRateLimitPort;
import com.vetsoftware.app.paymentgateway.domain.PaymentSourceRateLimitExceededException;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.RedisException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reutiliza el {@code ProxyManager} de bucket4j ya cableado para el rate
 * limiting de login ({@code RateLimitConfig} sobre Redis,
 * {@code InMemoryRateLimitConfig} en memoria si Redis está apagado): mismo
 * mecanismo, cubo propio por empresa.
 *
 * <p>
 * <strong>Fail-open ante un Valkey caído, y es a propósito.</strong> A
 * diferencia del tope de gasto de IA —que es fail-closed porque al otro lado
 * hay dinero saliendo—, aquí lo que protege el límite es un endpoint
 * <em>autenticado</em> de un empleado ya identificado: negarle crear un medio
 * de pago porque Valkey está caído degrada la disponibilidad de una operación
 * legítima a cambio de una ventana de abuso acotada y ya auditada. Se captura
 * {@link RedisException} —la superclase de las fallas de conexión y de timeout
 * de Lettuce— y no {@code RuntimeException} a secas, porque esta última también
 * envolvería a {@link PaymentSourceRateLimitExceededException}, que no es un
 * fallo de Redis sino la señal de que el límite sí se pudo consultar y sí se
 * agotó.
 */
@Component
public class Bucket4jPaymentSourceRateLimitPort implements PaymentSourceRateLimitPort {

    private static final Logger log = LoggerFactory
            .getLogger(Bucket4jPaymentSourceRateLimitPort.class);

    private static final String KEY_PREFIX = "payment-source-rl:";

    private final ProxyManager<String> proxyManager;
    private final PaymentGatewayMetrics metrics;
    private final int maxAttempts;
    private final Duration window;

    public Bucket4jPaymentSourceRateLimitPort(ProxyManager<String> loginRateLimitProxyManager,
            PaymentGatewayMetrics metrics,
            @Value("${vetsoftware.payments.wompi.payment-source-rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${vetsoftware.payments.wompi.payment-source-rate-limit.window:1h}") Duration window) {
        this.proxyManager = loginRateLimitProxyManager;
        this.metrics = metrics;
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    @Override
    public void checkAndConsume(Long companyId) {
        try {
            BucketProxy bucket = proxyManager.builder()
                    .build(KEY_PREFIX + companyId,
                            () -> BucketConfiguration.builder().addLimit(limit -> limit
                                    .capacity(maxAttempts).refillIntervally(maxAttempts, window))
                                    .build());
            if (!bucket.tryConsume(1))
                throw new PaymentSourceRateLimitExceededException(companyId);
        } catch (RedisException fallo) {
            log.warn("No se pudo consultar el límite de payment-sources en Valkey; se deja pasar"
                    + " (empresa {}): {}", companyId, fallo.getMessage());
            metrics.recordRateLimitFailOpen();
        }
    }
}
