package com.vetsoftware.app.surgerytype.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SurgeryTypeHasActiveChildrenException")
class SurgeryTypeHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del tipo de cirugia y el tipo de hijo")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        assertThat(new SurgeryTypeHasActiveChildrenException(700L, "surgery"))
                .hasMessageContaining("700").hasMessageContaining("surgery");
    }
}
