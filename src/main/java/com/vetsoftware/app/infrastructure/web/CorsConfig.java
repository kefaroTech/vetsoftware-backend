package com.vetsoftware.app.infrastructure.web;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // ⛔ Retry-After: el valor YA viaja y ya es correcto -LoginRateLimitFilter lo
        // escribe con la ventana que rechazo, 3600 o 86400- pero sin exponerlo el
        // navegador lo descarta en una peticion cross-origin y el front solo puede
        // decir "vuelve mas tarde". Exponerlo no concede ninguna informacion nueva:
        // convierte una adivinanza en "dentro de una hora" o "manana".
        //
        // Lo que NO se expone, y es deliberado: el cupo restante. Un
        // X-RateLimit-Remaining convertiria el endpoint en un oraculo del estado de
        // los cubos para quien pruebe a ciegas.
        config.setExposedHeaders(
                List.of("Authorization", "X-Trace-Id", "X-Request-Id", "Retry-After"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(
                new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
