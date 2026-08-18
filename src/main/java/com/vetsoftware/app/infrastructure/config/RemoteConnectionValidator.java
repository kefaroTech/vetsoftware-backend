package com.vetsoftware.app.infrastructure.config;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
 * Impide que los perfiles compartidos arranquen accidentalmente contra una
 * dependencia local. Se ejecuta antes de crear DataSource, Redis y clientes
 * HTTP/AWS.
 */
@Component
@Profile({"dev", "prod"})
final class RemoteConnectionValidator
        implements
            BeanFactoryPostProcessor,
            EnvironmentAware,
            Ordered {

    private static final List<String> REQUIRED_REMOTE_URLS = List.of("spring.datasource.url",
            "spring.data.redis.url", "management.otlp.metrics.export.url",
            "management.opentelemetry.tracing.export.otlp.endpoint",
            "management.opentelemetry.logging.export.otlp.endpoint",
            "vetsoftware.registration.verification-base-url",
            "vetsoftware.password-reset.reset-base-url", "vetsoftware.code-recovery.login-url",
            "vetsoftware.employee.login-url");

    /**
     * Hosts que delatan una dependencia local y por tanto un despliegue mal
     * configurado.
     */
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0",
            "::1", "[::1]", "host.docker.internal");

    /**
     * Únicas propiedades que admiten un destino en loopback.
     *
     * <p>
     * La task de ECS lleva un <em>sidecar</em> colector OTel: el backend exporta
     * trazas y métricas a {@code http://localhost:4318/v1/{traces,metrics}} y es el
     * colector —no la aplicación— quien las reenvía a Grafana Cloud con cola
     * persistente en disco. Un sidecar es, por definición, un destino local: aquí
     * el loopback es la configuración correcta y no el despiste que esta clase
     * existe para atrapar.
     *
     * <p>
     * La excepción es deliberadamente estrecha y no debe crecer sin una razón del
     * mismo tipo. Todo lo demás —base de datos, Redis, orígenes CORS, enlaces de
     * correo y S3— sigue rechazando loopback exactamente igual que antes. En
     * particular {@code management.opentelemetry.logging.export.otlp.endpoint}
     * queda fuera a propósito: el sidecar no procesa logs (van por CloudWatch y
     * Firehose), así que su exportación OTLP apunta directo a Grafana Cloud y un
     * loopback ahí sí sería un error.
     */
    private static final Set<String> LOOPBACK_ALLOWED_PROPERTIES = Set.of(
            "management.otlp.metrics.export.url",
            "management.opentelemetry.tracing.export.otlp.endpoint");

    /**
     * Hosts que resuelven al propio contenedor y por tanto pueden ser el sidecar.
     *
     * <p>
     * Subconjunto estricto de {@link #LOCAL_HOSTS}: {@code 0.0.0.0} es una
     * dirección de escucha y no de destino, y {@code host.docker.internal} apunta
     * al anfitrión y no al sidecar. Ninguno de los dos describe esta topología, así
     * que siguen rechazados incluso en las propiedades de la lista blanca.
     */
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1",
            "[::1]");

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
        validateOptionalRemoteUrl("vetsoftware.storage.s3.endpoint",
                environment.getProperty("vetsoftware.storage.s3.endpoint"));
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
        if (isLocal(lowerHost) && !isTelemetrySidecar(property, lowerHost)) {
            throw new IllegalStateException(
                    "El perfil dev/prod no permite conexiones locales: " + property);
        }
    }

    private static boolean isLocal(String lowerHost) {
        return LOCAL_HOSTS.contains(lowerHost) || lowerHost.endsWith(".localhost");
    }

    /**
     * Ver {@link #LOOPBACK_ALLOWED_PROPERTIES}: la excepción del colector OTel
     * local.
     */
    private static boolean isTelemetrySidecar(String property, String lowerHost) {
        return LOOPBACK_ALLOWED_PROPERTIES.contains(property) && LOOPBACK_HOSTS.contains(lowerHost);
    }
}
