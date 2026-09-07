package com.vetsoftware.app.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bucket4j.caffeine.CaffeineProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@code CacheConfig} y {@code RateLimitConfig} necesitan Redis vivo para
 * arrancar sus beans, así que su rama con el interruptor en {@code true} no se
 * ejercita aquí con un {@link ApplicationContextRunner} —eso lo cubre la suite
 * de integración con Testcontainers—. Lo que sí es barato y se prueba aquí: que
 * las dos declaran el mismo condicional, y que el sustituto en memoria aparece
 * solo cuando el interruptor está en {@code false}.
 */
class RedisToggleConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(InMemoryRateLimitConfig.class);

    @Test
    @DisplayName("con el interruptor en false expone un ProxyManager en memoria (Caffeine)")
    void con_redis_desactivado_expone_proxy_manager_en_memoria() {
        contextRunner.withPropertyValues("vetsoftware.redis.enabled=false").run(context -> {
            assertThat(context).hasSingleBean(ProxyManager.class);
            assertThat(context.getBean(ProxyManager.class))
                    .isInstanceOf(CaffeineProxyManager.class);
        });
    }

    @Test
    @DisplayName("con el interruptor en true no registra el ProxyManager en memoria")
    void con_redis_activado_no_registra_proxy_manager_en_memoria() {
        contextRunner.withPropertyValues("vetsoftware.redis.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(ProxyManager.class));
    }

    @Test
    @DisplayName("sin declarar el interruptor tampoco registra el ProxyManager en memoria")
    void sin_declarar_el_interruptor_no_registra_proxy_manager_en_memoria() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ProxyManager.class));
    }

    @Nested
    @DisplayName("Los beans de Redis de CacheConfig y RateLimitConfig se apagan con el mismo interruptor")
    class CondicionDeLosBeansDeRedis {

        /**
         * {@code CacheConfig.RedisCacheBeans} y no {@code CacheConfig}: el
         * {@code @EnableCaching} de fuera queda sin condicionar a propósito (ver su
         * Javadoc) y solo los beans específicos de Redis viven en la clase anidada.
         */
        @Test
        @DisplayName("las dos declaran el mismo @ConditionalOnProperty")
        void las_dos_clases_declaran_el_mismo_condicional() {
            for (Class<?> clase : List.of(CacheConfig.RedisCacheBeans.class,
                    RateLimitConfig.class)) {
                ConditionalOnProperty condicion = clase.getAnnotation(ConditionalOnProperty.class);

                assertThat(condicion).as(clase.getSimpleName()).isNotNull();
                assertThat(condicion.name()).as(clase.getSimpleName())
                        .containsExactly("vetsoftware.redis.enabled");
                assertThat(condicion.havingValue()).as(clase.getSimpleName()).isEqualTo("true");
                assertThat(condicion.matchIfMissing()).as(clase.getSimpleName()).isTrue();
            }
        }
    }
}
