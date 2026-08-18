package com.vetsoftware.app.vaccinationtype.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VaccinationTypeNotFoundException")
class VaccinationTypeNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id que no se encontro")
    void el_mensaje_incluye_el_id_que_no_se_encontro() {
        assertThatThrownBy(() -> {
            throw new VaccinationTypeNotFoundException(42L);
        }).isInstanceOf(RuntimeException.class).hasMessageContaining("VaccinationType not found")
                .hasMessageContaining("42");
    }
}
