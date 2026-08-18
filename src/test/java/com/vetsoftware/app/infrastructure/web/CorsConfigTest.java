package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * {@code @Bean} puro: sin contexto Spring, se instancia la clase e inyecta el
 * campo {@code allowedOrigins} por reflexión (lo resuelve normalmente
 * {@code @Value}) y se afirma sobre la {@link CorsConfiguration} efectiva que
 * registra el filtro.
 */
@DisplayName("CorsConfig")
class CorsConfigTest {

    @Test
    @DisplayName("registra un filtro CORS con la máxima precedencia y los orígenes configurados")
    void registra_un_filtro_cors_con_la_maxima_precedencia() throws Exception {
        CorsConfig config = configWithOrigins("https://app.vetrina.co", "https://admin.vetrina.co");

        FilterRegistrationBean<CorsFilter> registration = config.corsFilter();

        assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(registration.getFilter()).isInstanceOf(CorsFilter.class);
    }

    @Test
    @DisplayName("la configuración CORS efectiva expone los orígenes, métodos y cabeceras esperados")
    void la_configuracion_cors_efectiva_expone_lo_esperado() throws Exception {
        CorsConfig config = configWithOrigins("https://app.vetrina.co");

        CorsConfiguration effective = effectiveConfiguration(config);

        assertThat(effective.getAllowedOrigins()).containsExactly("https://app.vetrina.co");
        assertThat(effective.getAllowedMethods()).containsExactlyInAnyOrder("GET", "POST", "PUT",
                "PATCH", "DELETE", "OPTIONS");
        assertThat(effective.getAllowedHeaders()).containsExactly("*");
        assertThat(effective.getExposedHeaders()).containsExactlyInAnyOrder("Authorization",
                "X-Trace-Id", "X-Request-Id");
        assertThat(effective.getAllowCredentials()).isTrue();
        assertThat(effective.getMaxAge()).isEqualTo(3600L);
    }

    private static CorsConfiguration effectiveConfiguration(CorsConfig config) throws Exception {
        FilterRegistrationBean<CorsFilter> registration = config.corsFilter();
        CorsFilter filter = registration.getFilter();
        Field sourceField = CorsFilter.class.getDeclaredField("configSource");
        sourceField.setAccessible(true);
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) sourceField
                .get(filter);
        return source.getCorsConfigurations().get("/**");
    }

    private static CorsConfig configWithOrigins(String... origins) throws Exception {
        CorsConfig config = new CorsConfig();
        Field field = CorsConfig.class.getDeclaredField("allowedOrigins");
        field.setAccessible(true);
        field.set(config, origins);
        return config;
    }
}
