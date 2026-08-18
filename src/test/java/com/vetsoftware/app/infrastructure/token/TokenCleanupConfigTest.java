package com.vetsoftware.app.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * {@code @Configuration(proxyBeanMethods = false)} sin {@code @Bean} propio: su
 * único contrato es habilitar {@link TokenCleanupProperties} como
 * {@code @ConfigurationProperties}.
 */
@DisplayName("TokenCleanupConfig")
class TokenCleanupConfigTest {

    @Test
    @DisplayName("habilita TokenCleanupProperties como propiedades de configuración")
    void habilita_token_cleanup_properties_como_propiedades_de_configuracion() {
        EnableConfigurationProperties annotation = TokenCleanupConfig.class
                .getAnnotation(EnableConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(TokenCleanupProperties.class);
    }

    @Test
    @DisplayName("se instancia sin depender de ningún colaborador")
    void se_instancia_sin_depender_de_ningun_colaborador() {
        assertThat(new TokenCleanupConfig()).isNotNull();
    }
}
