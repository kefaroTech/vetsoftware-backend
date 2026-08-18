package com.vetsoftware.app.vaccinationtype.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VaccinationTypeHasActiveChildrenException")
class VaccinationTypeHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del tipo y el tipo de hijo que lo bloquea")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        assertThatThrownBy(() -> {
            throw new VaccinationTypeHasActiveChildrenException(5L, "vaccination");
        }).isInstanceOf(RuntimeException.class).hasMessageContaining("vaccinationtype 5")
                .hasMessageContaining("has active vaccination children");
    }
}
