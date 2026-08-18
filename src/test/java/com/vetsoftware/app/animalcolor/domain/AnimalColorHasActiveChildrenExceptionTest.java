package com.vetsoftware.app.animalcolor.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalColorHasActiveChildrenException — mensaje de dominio")
class AnimalColorHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del color y el tipo de hijo que lo bloquea")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        AnimalColorHasActiveChildrenException ex = new AnimalColorHasActiveChildrenException(5L,
                "animal");

        assertThat(ex.getMessage()).contains("5").contains("animal");
    }
}
