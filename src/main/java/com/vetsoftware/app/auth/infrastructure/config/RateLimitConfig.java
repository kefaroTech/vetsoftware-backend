package com.vetsoftware.app.auth.infrastructure.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.resource.ClientResources;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Almacén distribuido (Redis) para el rate limiting de login.
 *
 * <p>
 * Reemplaza el {@code ConcurrentHashMap} en-memoria que tenía dos problemas:
 * crecía sin límite (una entrada por IP, nunca se purgaba) y era por-instancia
 * (no protegía entre réplicas). Con bucket4j sobre Redis los buckets se
 * comparten entre réplicas y expiran solos por TTL.
 *
 * <p>
 * Usa un {@code RedisClient} Lettuce propio (con codec {@code byte[]} que
 * bucket4j requiere), reutilizando el {@code ClientResources} con tracing de
 * Micrometer ya definido en {@code
 * CacheConfig}.
 */
@Configuration
public class RateLimitConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient(ClientResources clientResources,
            @Value("${spring.data.redis.url:}") String url,
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        RedisURI redisUri = url == null || url.isBlank()
                ? RedisURI.builder().withHost(host).withPort(port).build()
                : RedisURI.create(url);
        return RedisClient.create(clientResources, redisUri);
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(
            RedisClient rateLimitRedisClient) {
        return rateLimitRedisClient
                .connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    public LettuceBasedProxyManager<String> loginRateLimitProxyManager(
            StatefulRedisConnection<String, byte[]> rateLimitRedisConnection) {
        // TTL de la clave basado en el tiempo de recarga del bucket → los buckets
        // ociosos se evictan solos de Redis (no hay fuga).
        //
        // ⛔ EL MARGEN ES DE UN DIA, NO DE UNA HORA, Y ES OBLIGATORIO desde que
        // `/assistant/proposal` declara ventanas diarias (20/dia por IP, 3/dia por
        // correo, 500/dia globales). El margen se suma al tiempo de recarga del
        // cubo, y quedarse corto no da un error: la clave del cubo diario expiraria
        // antes de que su ventana se cierre, el cubo renaceria lleno y el limite
        // diario dejaria de existir EN SILENCIO — que es exactamente el modo de
        // fallo que un control de gasto no puede permitirse. El coste de ampliarlo
        // es que las claves ociosas viven mas en Valkey: unos cientos de bytes por
        // IP activa, contra un techo de gasto que no se aplica.
        return Bucket4jLettuce.casBasedBuilder(rateLimitRedisConnection)
                .expirationAfterWrite(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofDays(1)))
                .build();
    }
}
