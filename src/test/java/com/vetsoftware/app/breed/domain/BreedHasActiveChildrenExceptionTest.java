package com.vetsoftware.app.breed.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BreedHasActiveChildrenException — mensaje de dominio")
class BreedHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id de la raza y el tipo de hijo que la bloquea")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        BreedHasActiveChildrenException ex = new BreedHasActiveChildrenException(5L, "animal");

        assertThat(ex.getMessage()).contains("5").contains("animal");
    }
}
