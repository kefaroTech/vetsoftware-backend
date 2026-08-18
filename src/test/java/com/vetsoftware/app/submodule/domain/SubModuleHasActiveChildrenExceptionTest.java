package com.vetsoftware.app.submodule.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubModuleHasActiveChildrenException — mensaje de dominio")
class SubModuleHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del submodulo y el tipo de hijo que lo bloquea")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        SubModuleHasActiveChildrenException ex = new SubModuleHasActiveChildrenException(5L,
                "membershipSubModule");

        assertThat(ex.getMessage()).contains("5").contains("membershipSubModule");
    }
}
