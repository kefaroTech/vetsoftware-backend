package com.vetsoftware.app.auth.infrastructure.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = ActuatorSecurityConfigIntegrationTest.TestApplication.class,
    properties = {
      "server.servlet.context-path=/api/v1",
      "management.endpoints.web.exposure.include=health,metrics,prometheus",
      "management.endpoint.health.show-details=when-authorized",
      "management.endpoint.health.roles=OBSERVABILITY",
      "management.health.redis.enabled=false",
      "vetsoftware.observability.actuator.authentication.enabled=true",
      "vetsoftware.observability.actuator.authentication.username=prometheus",
      "vetsoftware.observability.actuator.authentication.password=test-secret-with-at-least-32-characters"
    })
@AutoConfigureMockMvc
class ActuatorSecurityConfigIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void publicHealthIsAvailableWithoutInternalDetails() throws Exception {
    mockMvc
        .perform(get("/api/v1/actuator/health").contextPath("/api/v1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.components").doesNotExist());
  }

  @Test
  void prometheusRejectsAnonymousRequests() throws Exception {
    mockMvc
        .perform(get("/api/v1/actuator/prometheus").contextPath("/api/v1"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void prometheusAcceptsOnlyTheDedicatedTechnicalAccount() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/actuator/prometheus")
                .contextPath("/api/v1")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    basicAuthorization("prometheus", "test-secret-with-at-least-32-characters")))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/actuator/prometheus")
                .contextPath("/api/v1")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    basicAuthorization("prometheus", "incorrect-password")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void metricsRejectsAnonymousRequests() throws Exception {
    mockMvc
        .perform(get("/api/v1/actuator/metrics").contextPath("/api/v1"))
        .andExpect(status().isUnauthorized());
  }

  private static String basicAuthorization(String username, String password) {
    String credentials = username + ":" + password;
    return "Basic "
        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration(
      exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class,
        LiquibaseAutoConfiguration.class
      })
  @Import({SecurityAutoConfiguration.class, ActuatorSecurityConfig.class})
  static class TestApplication {}
}
