package com.vetsoftware.app.medicament.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MedicamentHasActiveChildrenException")
class MedicamentHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id y el tipo de hijo activo")
    void el_mensaje_incluye_id_y_tipo_de_hijo() {
        MedicamentHasActiveChildrenException exception = new MedicamentHasActiveChildrenException(
                5L, "medicamentPrescription");

        assertThat(exception.getMessage()).contains("5").contains("medicamentPrescription");
    }
}
