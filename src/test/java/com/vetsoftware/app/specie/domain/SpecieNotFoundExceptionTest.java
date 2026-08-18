package com.vetsoftware.app.specie.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpecieNotFoundException")
class SpecieNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje identifica el id de la especie no encontrada")
    void el_mensaje_identifica_el_id() {
        SpecieNotFoundException exception = new SpecieNotFoundException(999L);

        assertThat(exception.getMessage()).contains("Specie not found").contains("999");
    }
}
