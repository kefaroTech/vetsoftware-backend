package com.vetsoftware.app.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RedisDisabledEnvironmentPostProcessorTest {

    private final RedisDisabledEnvironmentPostProcessor processor = new RedisDisabledEnvironmentPostProcessor();

    @Nested
    @DisplayName("vetsoftware.redis.enabled=false")
    class RedisDesactivado {

        @Test
        @DisplayName("fuerza spring.cache.type=none y excluye la autoconfiguracion de Redis")
        void fuerza_cache_none_y_excluye_autoconfiguraciones() {
            MockEnvironment environment = new MockEnvironment()
                    .withProperty("vetsoftware.redis.enabled", "false");

            processor.postProcessEnvironment(environment, null);

            assertThat(environment.getProperty("spring.cache.type")).isEqualTo("none");
            assertThat(environment.getProperty("spring.autoconfigure.exclude").split(",")).contains(
                    "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
                    "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration",
                    "org.springframework.boot.data.redis.autoconfigure.health.DataRedisHealthContributorAutoConfiguration");
        }

        @Test
        @DisplayName("conserva las exclusiones que ya traiga el entorno")
        void conserva_exclusiones_previas() {
            MockEnvironment environment = new MockEnvironment()
                    .withProperty("vetsoftware.redis.enabled", "false").withProperty(
                            "spring.autoconfigure.exclude", "com.acme.AlgunaAutoConfiguration");

            processor.postProcessEnvironment(environment, null);

            assertThat(environment.getProperty("spring.autoconfigure.exclude").split(",")).contains(
                    "com.acme.AlgunaAutoConfiguration",
                    "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration");
        }
    }

    @Nested
    @DisplayName("vetsoftware.redis.enabled en true o ausente")
    class RedisActivado {

        @Test
        @DisplayName("no toca el entorno cuando el interruptor esta en true")
        void no_toca_el_entorno_con_redis_activo() {
            MockEnvironment environment = new MockEnvironment()
                    .withProperty("vetsoftware.redis.enabled", "true");

            processor.postProcessEnvironment(environment, null);

            assertThat(environment.getProperty("spring.cache.type")).isNull();
            assertThat(environment.getProperty("spring.autoconfigure.exclude")).isNull();
        }

        @Test
        @DisplayName("no toca el entorno cuando el interruptor no esta declarado")
        void no_toca_el_entorno_sin_declarar_el_interruptor() {
            MockEnvironment environment = new MockEnvironment();

            processor.postProcessEnvironment(environment, null);

            assertThat(environment.getProperty("spring.cache.type")).isNull();
            assertThat(environment.getProperty("spring.autoconfigure.exclude")).isNull();
        }
    }
}
