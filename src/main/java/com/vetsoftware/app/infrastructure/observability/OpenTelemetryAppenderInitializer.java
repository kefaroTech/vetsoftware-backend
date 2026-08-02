package com.vetsoftware.app.infrastructure.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/** Conecta Logback con el {@code SdkLoggerProvider} administrado por Spring Boot. */
@Component
public final class OpenTelemetryAppenderInitializer implements InitializingBean {

  private final OpenTelemetry openTelemetry;

  public OpenTelemetryAppenderInitializer(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  @Override
  public void afterPropertiesSet() {
    OpenTelemetryAppender.install(openTelemetry);
  }
}
