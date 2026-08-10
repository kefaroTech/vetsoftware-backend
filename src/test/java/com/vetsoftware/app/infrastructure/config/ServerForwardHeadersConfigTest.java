package com.vetsoftware.app.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

/**
 * Prueba de gobierno sobre {@code application.yml}: la resolución de la IP real
 * del cliente no puede depender de que alguien recuerde por qué está esa línea.
 *
 * <p>
 * <b>Qué protege.</b> El rate limiting agrupa por
 * {@code request.getRemoteAddr()}. Detrás del balanceador, esa llamada devuelve
 * la IP del <i>proxy</i> salvo que el contenedor reescriba la IP remota desde
 * {@code X-Forwarded-For}. Sin eso, el límite se aplica al balanceador y no al
 * atacante: o se bloquea a todos los clientes a la vez, o a ninguno. Es la
 * mitad de BE-15 que ya estaba resuelta —desde el 31 de mayo de 2026— y que
 * nadie estaba comprobando.
 *
 * <p>
 * <b>Por qué {@code native} y no {@code framework}.</b> {@code native} usa el
 * {@code RemoteIpValve} de Tomcat, que solo confía en proxies de rangos
 * privados: un cliente externo que mande su propio {@code X-Forwarded-For} no
 * se puede escapar del límite falseando su IP. Cambiarlo a {@code none}
 * reabriría BE-15 en silencio, y sin esta prueba el CI seguiría en verde.
 */
@DisplayName("application.yml — resolución de la IP real tras el proxy")
class ServerForwardHeadersConfigTest {

    private static final String PROPERTY = "server.forward-headers-strategy";

    private static String declaredValue() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Object value = yaml.getObject().get(PROPERTY);
        return value == null ? null : value.toString();
    }

    /** Resuelve {@code ${VAR:default}} al valor por defecto embebido. */
    private static String defaultOf(String placeholder) {
        int separator = placeholder.indexOf(':');
        return placeholder.substring(separator + 1, placeholder.length() - 1);
    }

    @Test
    @DisplayName("la estrategia está declarada y su valor por defecto es 'native'")
    void la_estrategia_por_defecto_es_native() {
        String declared = declaredValue();

        assertThat(declared).as(PROPERTY + " debe estar declarada en application.yml").isNotNull()
                .startsWith("${").endsWith("}");
        assertThat(defaultOf(declared))
                .as("sin esto getRemoteAddr() devuelve la IP del balanceador — ver BE-15")
                .isEqualTo("native");
    }

    @Test
    @DisplayName("sigue siendo sobreescribible por entorno, para desplegar sin proxy delante")
    void sigue_siendo_sobreescribible_por_entorno() {
        assertThat(declaredValue()).startsWith("${FORWARD_HEADERS_STRATEGY:");
    }

    @Test
    @DisplayName("el context-path no cambió: los patrones de ruta pública son relativos a él")
    void el_context_path_no_cambio() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Map<Object, Object> properties = yaml.getObject();

        assertThat(properties.get("server.servlet.context-path")).isEqualTo("/api/v1");
    }
}
