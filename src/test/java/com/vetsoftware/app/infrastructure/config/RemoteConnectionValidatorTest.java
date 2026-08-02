package com.vetsoftware.app.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

class RemoteConnectionValidatorTest {

    @Test
    void acceptsOnlyRemoteDependencies() {
        RemoteConnectionValidator validator = validatorWithValidRemoteEnvironment();

        assertThatCode(
                () -> validator.postProcessBeanFactory(mock(ConfigurableListableBeanFactory.class)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsLocalhostInDevOrProd() {
        RemoteConnectionValidator validator = validatorWithValidRemoteEnvironment();
        MockEnvironment environment = validRemoteEnvironment().withProperty("spring.data.redis.url",
                "redis://localhost:6379");
        validator.setEnvironment(environment);

        assertThatThrownBy(
                () -> validator.postProcessBeanFactory(mock(ConfigurableListableBeanFactory.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no permite conexiones locales")
                .hasMessageContaining("spring.data.redis.url");
    }

    @Test
    void requiresOtlpAuthenticationHeaders() {
        RemoteConnectionValidator validator = validatorWithValidRemoteEnvironment();
        MockEnvironment environment = validRemoteEnvironment()
                .withProperty("OTEL_EXPORTER_OTLP_HEADERS", "");
        validator.setEnvironment(environment);

        assertThatThrownBy(
                () -> validator.postProcessBeanFactory(mock(ConfigurableListableBeanFactory.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OTEL_EXPORTER_OTLP_HEADERS");
    }

    private static RemoteConnectionValidator validatorWithValidRemoteEnvironment() {
        RemoteConnectionValidator validator = new RemoteConnectionValidator();
        validator.setEnvironment(validRemoteEnvironment());
        return validator;
    }

    private static MockEnvironment validRemoteEnvironment() {
        return new MockEnvironment()
                .withProperty("spring.datasource.url",
                        "jdbc:mysql://db.dev.example.com:3306/vetsoftware")
                .withProperty("spring.data.redis.url", "rediss://redis.dev.example.com:6380")
                .withProperty("management.otlp.metrics.export.url",
                        "https://otlp.example.com/otlp/v1/metrics")
                .withProperty("management.opentelemetry.tracing.export.otlp.endpoint",
                        "https://otlp.example.com/otlp/v1/traces")
                .withProperty("management.opentelemetry.logging.export.otlp.endpoint",
                        "https://otlp.example.com/otlp/v1/logs")
                .withProperty("vetsoftware.registration.verification-base-url",
                        "https://app.dev.example.com/verify-email")
                .withProperty("vetsoftware.password-reset.reset-base-url",
                        "https://app.dev.example.com/reset-password")
                .withProperty("vetsoftware.code-recovery.login-url",
                        "https://app.dev.example.com/login")
                .withProperty("vetsoftware.employee.login-url", "https://app.dev.example.com/login")
                .withProperty("cors.allowed-origins",
                        "https://app.dev.example.com,https://admin.dev.example.com")
                .withProperty("OTEL_EXPORTER_OTLP_HEADERS", "Authorization=Basic%20test");
    }
}
