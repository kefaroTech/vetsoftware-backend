package com.vetsoftware.app.consultation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationHasActiveChildrenException")
class ConsultationHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id de la consulta y el tipo de hijo bloqueante")
    void el_mensaje_incluye_id_y_tipo_de_hijo() {
        ConsultationHasActiveChildrenException exception = new ConsultationHasActiveChildrenException(
                200L, "vaccination");

        assertThat(exception.getMessage())
                .isEqualTo("Cannot delete consultation 200: has active vaccination children");
    }
}
