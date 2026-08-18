package com.vetsoftware.app.numberingresolution.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NumberingResolutionNotFoundException")
class NumberingResolutionNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id buscado")
    void el_mensaje_incluye_el_id_buscado() {
        NumberingResolutionNotFoundException ex = new NumberingResolutionNotFoundException(700L);

        assertThat(ex.getMessage()).isEqualTo("Numbering resolution not found: 700");
    }
}
