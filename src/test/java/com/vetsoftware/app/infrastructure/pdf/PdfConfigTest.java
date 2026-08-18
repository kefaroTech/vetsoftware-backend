package com.vetsoftware.app.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * {@code @Configuration(proxyBeanMethods = false)} sin {@code @Bean} propio: su
 * único contrato es habilitar {@link PdfProperties} como
 * {@code @ConfigurationProperties}. Se verifica declarativamente, sin contexto
 * Spring — levantarlo aquí sería probar el framework, no el código.
 */
@DisplayName("PdfConfig")
class PdfConfigTest {

    @Test
    @DisplayName("habilita PdfProperties como propiedades de configuración")
    void habilita_pdf_properties_como_propiedades_de_configuracion() {
        EnableConfigurationProperties annotation = PdfConfig.class
                .getAnnotation(EnableConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(PdfProperties.class);
    }

    @Test
    @DisplayName("se instancia sin depender de ningún colaborador")
    void se_instancia_sin_depender_de_ningun_colaborador() {
        assertThat(new PdfConfig()).isNotNull();
    }
}
