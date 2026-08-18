package com.vetsoftware.app.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    /**
     * El sidecar colector OTel de la task de ECS escucha en el loopback del propio
     * contenedor: para trazas y métricas ese destino es la configuración correcta,
     * no un despiste.
     */
    @Test
    void acceptsLoopbackOnTelemetrySidecarEndpoints() {
        RemoteConnectionValidator validator = validatorFor(validRemoteEnvironment()
                .withProperty("management.otlp.metrics.export.url",
                        "http://127.0.0.1:4318/v1/metrics")
                .withProperty("management.opentelemetry.tracing.export.otlp.endpoint",
                        "http://localhost:4318/v1/traces"));

        assertThatCode(
                () -> validator.postProcessBeanFactory(mock(ConfigurableListableBeanFactory.class)))
                .doesNotThrowAnyException();
    }

    /**
     * La contracara del test anterior y la que sostiene el valor del cambio: la
     * excepción del sidecar es una lista blanca de dos propiedades, no un
     * relajamiento de la regla general. Si alguien simplifica el arreglo aflojando
     * la validación, esto se pone rojo.
     */
    @ParameterizedTest
    @CsvSource({"spring.datasource.url, jdbc:mysql://localhost:3306/vetsoftware",
            "spring.data.redis.url, redis://127.0.0.1:6379",
            "management.opentelemetry.logging.export.otlp.endpoint, http://localhost:4318/v1/logs",
            "cors.allowed-origins, http://localhost:5173",
            "vetsoftware.registration.verification-base-url, http://localhost:5173/verify-email",
            "vetsoftware.password-reset.reset-base-url, http://localhost:5173/reset-password",
            "vetsoftware.code-recovery.login-url, http://localhost:5173/login",
            "vetsoftware.employee.login-url, http://127.0.0.1:5173/login",
            "vetsoftware.storage.s3.endpoint, http://127.0.0.1:9000",
            "vetsoftware.audit.outbox.endpoint, http://localhost:4566"})
    void rejectsLoopbackOnEveryConnectionOutsideTheSidecarAllowList(String property,
            String localUrl) {
        RemoteConnectionValidator validator = validatorFor(
                validRemoteEnvironment().withProperty(property, localUrl));

        assertThatThrownBy(
                () -> validator.postProcessBeanFactory(mock(ConfigurableListableBeanFactory.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no permite conexiones locales")
                .hasMessageContaining(property);
    }

    /**
     * La excepción cubre el loopback real del contenedor y nada más: 0.0.0.0 es una
     * dirección de escucha, host.docker.internal apunta al anfitrión y un
     * subdominio .localhost no es el sidecar.
     */
    @ParameterizedTest
    @ValueSource(strings = {"http://0.0.0.0:4318/v1/traces",
            "http://host.docker.internal:4318/v1/traces", "http://otel.localhost:4318/v1/traces"})
    void rejectsNonLoopbackLocalHostsEvenOnAllowedTelemetryProperties(String endpoint) {
        RemoteConnectionValidator validator = validatorFor(validRemoteEnvironment()
                .withProperty("management.opentelemetry.tracing.export.otlp.endpoint", endpoint));

        assertThatThrownBy(
                () -> validator.postProcessBeanFactory(mock(ConfigurableListableBeanFactory.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no permite conexiones locales")
                .hasMessageContaining("management.opentelemetry.tracing.export.otlp.endpoint");
    }

    /**
     * El sidecar no exime de autenticar: en la rama sin colector local sigue
     * haciendo falta.
     */
    @Test
    void stillRequiresOtlpAuthenticationHeadersWithTheLocalCollector() {
        RemoteConnectionValidator validator = validatorFor(validRemoteEnvironment()
                .withProperty("management.otlp.metrics.export.url",
                        "http://localhost:4318/v1/metrics")
                .withProperty("management.opentelemetry.tracing.export.otlp.endpoint",
                        "http://localhost:4318/v1/traces")
                .withProperty("OTEL_EXPORTER_OTLP_HEADERS", ""));

        assertThatThrownBy(
                () -> validator.postProcessBeanFactory(mock(ConfigurableListableBeanFactory.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OTEL_EXPORTER_OTLP_HEADERS");
    }

    private static RemoteConnectionValidator validatorWithValidRemoteEnvironment() {
        return validatorFor(validRemoteEnvironment());
    }

    private static RemoteConnectionValidator validatorFor(MockEnvironment environment) {
        RemoteConnectionValidator validator = new RemoteConnectionValidator();
        validator.setEnvironment(environment);
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
