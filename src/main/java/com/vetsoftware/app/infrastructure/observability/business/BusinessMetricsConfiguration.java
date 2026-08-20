package com.vetsoftware.app.infrastructure.observability.business;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessMetricsConfiguration {

    /**
     * Se declara con el tipo concreto y no como {@code MeterFilter} para que Spring
     * vea también su cara de {@code MeterBinder}: el mismo bean filtra las
     * etiquetas de negocio y publica el contador de descartes
     * {@link BusinessMetricCardinalityFilter#DENIED}. El orden lo garantiza
     * {@code MeterRegistryConfigurer}, que aplica los filtros antes que los
     * binders; el binder no se aplica a los registros compuestos, así que cada
     * registro real publica su propio contador sobre los mismos acumuladores.
     *
     * <p>
     * El bean no puede depender del {@code MeterRegistry}: los {@code MeterFilter}
     * se aplican mientras el registro se está construyendo y la dependencia sería
     * un ciclo.
     */
    @Bean
    public BusinessMetricCardinalityFilter businessMetricCardinalityFilter() {
        return new BusinessMetricCardinalityFilter();
    }
}
