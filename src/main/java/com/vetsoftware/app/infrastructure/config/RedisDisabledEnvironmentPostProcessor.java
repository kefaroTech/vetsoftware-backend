package com.vetsoftware.app.infrastructure.config;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Apaga la autoconfiguración de Redis de Boot cuando
 * {@code vetsoftware.redis.enabled} vale {@code false}.
 *
 * <p>
 * El Valkey de dev se apaga para ahorrar coste, y la aplicación tiene que
 * seguir arrancando sana en ese entorno. Las tres autoconfiguraciones de
 * {@link #REDIS_AUTOCONFIGURATIONS} —Boot 4.1 las movió al paquete
 * {@code org.springframework.boot.data.redis.autoconfigure} y les cambió el
 * nombre de clase— no llevan un {@code @ConditionalOnProperty} propio con el
 * que engancharse, así que se excluyen desde aquí antes de que
 * {@code AutoConfigurationImportSelector} las procese. Sin la exclusión, su
 * indicador de salud intentaría un {@code PING} contra un Redis inexistente y
 * pondría {@code DOWN} cualquier lectura de {@code /actuator/health} que
 * incluya sus contribuidores.
 *
 * <p>
 * Se resuelve aquí también {@code spring.cache.type=none}: es una decisión que
 * depende del mismo interruptor, y una propiedad calculada en tiempo de
 * arranque no puede expresarse como placeholder de YAML. Los métodos
 * {@code @Cacheable} de la aplicación se quedan; con este valor, Spring los
 * deja pasar sin cachear en vez de fallar al buscar un {@code CacheManager} de
 * Redis que no existe.
 */
public class RedisDisabledEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String ENABLED_PROPERTY = "vetsoftware.redis.enabled";

    private static final Set<String> REDIS_AUTOCONFIGURATIONS = Set.of(
            "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
            "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration",
            "org.springframework.boot.data.redis.autoconfigure.health.DataRedisHealthContributorAutoConfiguration");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
            SpringApplication application) {
        if (environment.getProperty(ENABLED_PROPERTY, Boolean.class, true)) {
            return;
        }
        Set<String> exclusions = new LinkedHashSet<>(REDIS_AUTOCONFIGURATIONS);
        String existing = environment.getProperty("spring.autoconfigure.exclude");
        if (existing != null && !existing.isBlank()) {
            exclusions.addAll(Set.of(existing.split(",")));
        }
        environment.getPropertySources().addFirst(
                new MapPropertySource("vetsoftwareRedisDisabled", Map.of("spring.cache.type",
                        "none", "spring.autoconfigure.exclude", String.join(",", exclusions))));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
