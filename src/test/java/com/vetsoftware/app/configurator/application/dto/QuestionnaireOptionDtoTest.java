package com.vetsoftware.app.configurator.application.dto;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La opción tal como la ve el prospecto. No lleva {@code questionId}: la
 * jerarquía ya la da el anidamiento dentro de su pregunta, y repetirla en el
 * JSON público solo da otra forma de que las dos discrepen.
 */
@DisplayName("QuestionnaireOptionDto — la opcion tal como la ve el prospecto")
class QuestionnaireOptionDtoTest {

    @Test
    @DisplayName("copia lo que el prospecto necesita para elegir")
    void copia_lo_que_el_prospecto_necesita() {
        QuestionnaireOptionDto dto = QuestionnaireOptionDto
                .from(opcion(O11_SI_VENDE, Q1_VENDE, "YES"));

        assertThat(dto.id()).isEqualTo(O11_SI_VENDE);
        assertThat(dto.code()).isEqualTo("YES");
        assertThat(dto.label()).isEqualTo("YES");
        assertThat(dto.helpText()).isNull();
        assertThat(dto.sortOrder()).isZero();
    }

    @Test
    @DisplayName("conserva la ayuda y el orden cuando los hay")
    void conserva_la_ayuda_y_el_orden() {
        QuestionnaireOptionDto dto = QuestionnaireOptionDto
                .from(new ConfiguratorOption(O11_SI_VENDE, Q1_VENDE, "YES", "Si, vendo",
                        "Marque si factura", 3, CREADA_EL, 0L, true));

        assertThat(dto.label()).isEqualTo("Si, vendo");
        assertThat(dto.helpText()).isEqualTo("Marque si factura");
        assertThat(dto.sortOrder()).isEqualTo(3);
    }
}
