package com.vetsoftware.app.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@DisplayName("OpenTelemetryAppenderInitializer")
class OpenTelemetryAppenderInitializerTest {

    @Test
    @DisplayName("instala el SdkLoggerProvider administrado por Spring en el appender de Logback")
    void instala_el_sdk_logger_provider_en_el_appender_de_logback() {
        OpenTelemetry openTelemetry = mock(OpenTelemetry.class);
        OpenTelemetryAppenderInitializer initializer = new OpenTelemetryAppenderInitializer(
                openTelemetry);

        try (MockedStatic<OpenTelemetryAppender> appender = mockStatic(
                OpenTelemetryAppender.class)) {
            assertThatCode(initializer::afterPropertiesSet).doesNotThrowAnyException();

            appender.verify(() -> OpenTelemetryAppender.install(openTelemetry));
        }
    }
}
