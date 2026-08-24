package com.vetsoftware.app.configurator.application.dto;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Proyección administrativa de una opción. */
@DisplayName("ConfiguratorOptionDto — proyeccion de una opcion")
class ConfiguratorOptionDtoTest {

    @Test
    @DisplayName("copia campo por campo, incluida la pregunta a la que pertenece")
    void copia_campo_por_campo() {
        ConfiguratorOptionDto dto = ConfiguratorOptionDto
                .from(opcion(O11_SI_VENDE, Q1_VENDE, "YES"));

        assertThat(dto.id()).isEqualTo(O11_SI_VENDE);
        assertThat(dto.questionId()).isEqualTo(Q1_VENDE);
        assertThat(dto.code()).isEqualTo("YES");
        assertThat(dto.label()).isEqualTo("YES");
        assertThat(dto.helpText()).isNull();
        assertThat(dto.sortOrder()).isZero();
        assertThat(dto.createdDate()).isEqualTo(CREADA_EL);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("no se inventa que una opcion dada de baja sigue activa")
    void no_se_inventa_que_una_opcion_de_baja_sigue_activa() {
        ConfiguratorOption dadaDeBaja = new ConfiguratorOption(O11_SI_VENDE, Q1_VENDE, "YES", "Si",
                "ayuda", 2, CREADA_EL, 1L, false);

        ConfiguratorOptionDto dto = ConfiguratorOptionDto.from(dadaDeBaja);

        assertThat(dto.enabled()).isFalse();
        assertThat(dto.helpText()).isEqualTo("ayuda");
        assertThat(dto.sortOrder()).isEqualTo(2);
    }
}
