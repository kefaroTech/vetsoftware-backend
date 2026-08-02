package com.vetsoftware.app.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.observation.ServerRequestObservationContext;

class ObservabilityConfigTest {

  private final ObservationPredicate predicate =
      new ObservabilityConfig().productionActuatorObservationPredicate();

  @Test
  void excludesPrometheusAndHealthButKeepsBusinessTraffic() {
    assertThat(predicate.test("http.server.requests", context("/api/v1/actuator/prometheus")))
        .isFalse();
    assertThat(predicate.test("http.server.requests", context("/api/v1/actuator/health/readiness")))
        .isFalse();
    assertThat(predicate.test("http.server.requests", context("/api/v1/appointments"))).isTrue();
  }

  @Test
  void keepsNonHttpObservations() {
    assertThat(predicate.test("vetsoftware.job", new Observation.Context())).isTrue();
  }

  private static ServerRequestObservationContext context(String uri) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(uri);
    return new ServerRequestObservationContext(request, mock(HttpServletResponse.class));
  }
}
