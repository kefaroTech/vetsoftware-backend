package com.vetsoftware.app.infrastructure.config;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
public class ObservabilityConfig {

  @Bean
  public ObservedAspect observedAspect(ObservationRegistry registry) {
    return new ObservedAspect(registry);
  }

  /**
   * Los scrapes y probes son tráfico interno periódico: conservarlos al 100 % consumiría cuota de
   * trazas sin aportar contexto de negocio.
   */
  @Bean
  @Profile("prod")
  public ObservationPredicate productionActuatorObservationPredicate() {
    return (name, context) -> {
      if (!(context instanceof ServerRequestObservationContext serverContext)) {
        return true;
      }
      String path = serverContext.getCarrier().getRequestURI();
      return !path.endsWith("/actuator/prometheus")
          && !path.endsWith("/actuator/health")
          && !path.contains("/actuator/health/");
    };
  }
}
