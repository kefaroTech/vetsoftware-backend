package com.vetsoftware.app.animalcolor.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalColorNotFoundException — mensaje de dominio")
class AnimalColorNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del color no encontrado")
    void el_mensaje_incluye_el_id_del_color_no_encontrado() {
        AnimalColorNotFoundException ex = new AnimalColorNotFoundException(42L);

        assertThat(ex.getMessage()).contains("42").contains("AnimalColor not found");
    }
}
