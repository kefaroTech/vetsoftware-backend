package com.vetsoftware.app.infrastructure.config;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Impide que los perfiles compartidos arranquen accidentalmente contra una dependencia local. Se
 * ejecuta antes de crear DataSource, Redis y clientes HTTP/AWS.
 */
@Component
@Profile({"dev", "prod"})
final class RemoteConnectionValidator
    implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

  private static final List<String> REQUIRED_REMOTE_URLS =
      List.of(
          "spring.datasource.url",
          "spring.data.redis.url",
          "management.otlp.metrics.export.url",
          "management.opentelemetry.tracing.export.otlp.endpoint",
          "management.opentelemetry.logging.export.otlp.endpoint",
          "vetsoftware.registration.verification-base-url",
          "vetsoftware.password-reset.reset-base-url",
          "vetsoftware.code-recovery.login-url",
          "vetsoftware.employee.login-url");

  private Environment environment;

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {
    for (String property : REQUIRED_REMOTE_URLS) {
      validateRemoteUrl(property, required(property));
    }
    for (String origin : required("cors.allowed-origins").split(",")) {
      validateRemoteUrl("cors.allowed-origins", origin.trim());
    }
    validateOptionalRemoteUrl(
        "vetsoftware.storage.s3.endpoint",
        environment.getProperty("vetsoftware.storage.s3.endpoint"));
    validateOptionalRemoteUrl(
        "vetsoftware.audit.outbox.endpoint",
        environment.getProperty("vetsoftware.audit.outbox.endpoint"));
    required("OTEL_EXPORTER_OTLP_HEADERS");
  }

  private String required(String property) {
    String value = environment.getProperty(property);
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException(
          "El perfil remoto requiere la propiedad/variable: " + property);
    }
    return value.trim();
  }

  private static void validateOptionalRemoteUrl(String property, String value) {
    if (StringUtils.hasText(value)) {
      validateRemoteUrl(property, value.trim());
    }
  }

  private static void validateRemoteUrl(String property, String value) {
    String normalized = value.startsWith("jdbc:") ? value.substring(5) : value;
    URI uri;
    try {
      uri = URI.create(normalized);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("URL inválida para " + property, exception);
    }
    String host = uri.getHost();
    if (!StringUtils.hasText(host)) {
      throw new IllegalStateException("La propiedad " + property + " no contiene un host");
    }
    String lowerHost = host.toLowerCase(Locale.ROOT);
    if (lowerHost.equals("localhost")
        || lowerHost.equals("127.0.0.1")
        || lowerHost.equals("0.0.0.0")
        || lowerHost.equals("::1")
        || lowerHost.equals("host.docker.internal")
        || lowerHost.endsWith(".localhost")) {
      throw new IllegalStateException(
          "El perfil dev/prod no permite conexiones locales: " + property);
    }
  }
}
