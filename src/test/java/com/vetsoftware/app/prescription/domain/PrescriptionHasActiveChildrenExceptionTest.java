package com.vetsoftware.app.prescription.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PrescriptionHasActiveChildrenException")
class PrescriptionHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id y el tipo de hijo activo")
    void el_mensaje_incluye_id_y_tipo_de_hijo() {
        PrescriptionHasActiveChildrenException exception = new PrescriptionHasActiveChildrenException(
                7L, "medicamentPrescription");

        assertThat(exception.getMessage()).contains("7").contains("medicamentPrescription");
    }
}
