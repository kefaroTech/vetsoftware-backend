package com.vetsoftware.app.infrastructure.observability.business;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.config.MeterFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BusinessMetricsConfiguration")
class BusinessMetricsConfigurationTest {

    @Test
    @DisplayName("registra el filtro de cardinalidad de métricas de negocio")
    void registra_el_filtro_de_cardinalidad_de_metricas_de_negocio() {
        MeterFilter filter = new BusinessMetricsConfiguration().businessMetricCardinalityFilter();

        assertThat(filter).isInstanceOf(BusinessMetricCardinalityFilter.class);
    }
}
