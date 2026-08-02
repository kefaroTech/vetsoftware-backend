package com.vetsoftware.app.infrastructure.observability.business;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessMetricsConfiguration {

  @Bean
  public MeterFilter businessMetricCardinalityFilter() {
    return new BusinessMetricCardinalityFilter();
  }
}
