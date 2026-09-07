package com.vetsoftware.app.auth.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.caffeine.Bucket4jCaffeine;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sustituto en memoria de {@link RateLimitConfig} cuando Valkey está apagado.
 *
 * <p>
 * Degradación aceptada: los cubos ya no se comparten entre réplicas, así que un
 * atacante distribuido entre instancias consigue el límite multiplicado por el
 * número de réplicas. Es admisible en dev, que corre con una sola réplica de
 * ECS; si este perfil llegara a correr con más de una, el rate limiting deja de
 * ser fiable.
 */
@Configuration
@ConditionalOnProperty(name = "vetsoftware.redis.enabled", havingValue = "false")
public class InMemoryRateLimitConfig {

    @Bean
    public ProxyManager<String> loginRateLimitProxyManager() {
        return Bucket4jCaffeine.<String>builderFor(Caffeine.newBuilder())
                .expirationAfterWrite(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofDays(1)))
                .build();
    }
}
