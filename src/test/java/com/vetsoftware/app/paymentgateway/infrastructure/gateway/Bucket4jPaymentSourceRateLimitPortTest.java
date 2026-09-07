package com.vetsoftware.app.paymentgateway.infrastructure.gateway;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics;
import com.vetsoftware.app.paymentgateway.domain.PaymentSourceRateLimitExceededException;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.lettuce.core.RedisConnectionException;
import java.time.Duration;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Bucket4jPaymentSourceRateLimitPort")
class Bucket4jPaymentSourceRateLimitPortTest {

    private static final Long EMPRESA = 42L;

    @Mock
    private ProxyManager<String> proxyManager;
    @Mock
    private RemoteBucketBuilder<String> remoteBucketBuilder;
    @Mock
    private BucketProxy bucket;
    @Mock
    private PaymentGatewayMetrics metrics;

    private Bucket4jPaymentSourceRateLimitPort port;

    @BeforeEach
    void montar() {
        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        // build(K, Supplier) y build(K, BucketConfiguration) son ambiguos para any():
        // se fija el tipo del matcher para que el compilador elija el overload
        // correcto.
        when(remoteBucketBuilder.build(anyString(),
                ArgumentMatchers.<Supplier<BucketConfiguration>>any())).thenReturn(bucket);
        port = new Bucket4jPaymentSourceRateLimitPort(proxyManager, metrics, 5,
                Duration.ofHours(1));
    }

    @Test
    @DisplayName("con cupo disponible no revienta")
    void con_cupo_disponible_no_revienta() {
        when(bucket.tryConsume(1)).thenReturn(true);

        assertThatCode(() -> port.checkAndConsume(EMPRESA)).doesNotThrowAnyException();

        verifyNoInteractions(metrics);
    }

    @Test
    @DisplayName("sin cupo revienta con PaymentSourceRateLimitExceededException")
    void sin_cupo_revienta() {
        when(bucket.tryConsume(1)).thenReturn(false);

        assertThatThrownBy(() -> port.checkAndConsume(EMPRESA))
                .isInstanceOf(PaymentSourceRateLimitExceededException.class);

        verifyNoInteractions(metrics);
    }

    @Nested
    @DisplayName("Valkey caido (SEC2-12)")
    class ValkeyCaido {

        @Test
        @DisplayName("una excepcion de conexion de Redis deja pasar la peticion y cuenta la metrica de fail-open")
        void redis_caido_deja_pasar_y_cuenta_metrica() {
            when(bucket.tryConsume(1))
                    .thenThrow(new RedisConnectionException("timeout conectando a Valkey"));

            assertThatCode(() -> port.checkAndConsume(EMPRESA)).doesNotThrowAnyException();

            verify(metrics).recordRateLimitFailOpen();
        }
    }
}
