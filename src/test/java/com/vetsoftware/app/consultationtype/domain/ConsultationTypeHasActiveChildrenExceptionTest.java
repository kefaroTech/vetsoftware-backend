package com.vetsoftware.app.consultationtype.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationTypeHasActiveChildrenException")
class ConsultationTypeHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del tipo y el tipo de hijo que lo bloquea")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        assertThatThrownBy(() -> {
            throw new ConsultationTypeHasActiveChildrenException(5L, "consultation");
        }).isInstanceOf(RuntimeException.class).hasMessageContaining("consultationtype 5")
                .hasMessageContaining("has active consultation children");
    }
}
