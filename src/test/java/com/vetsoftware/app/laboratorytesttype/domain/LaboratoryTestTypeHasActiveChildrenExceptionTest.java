package com.vetsoftware.app.laboratorytesttype.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LaboratoryTestTypeHasActiveChildrenException")
class LaboratoryTestTypeHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del tipo y el tipo de hijo activo")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        LaboratoryTestTypeHasActiveChildrenException ex = new LaboratoryTestTypeHasActiveChildrenException(
                70L, "laboratoryTest");

        assertThat(ex.getMessage()).contains("laboratorytesttype 70")
                .contains("has active laboratoryTest children");
    }
}
