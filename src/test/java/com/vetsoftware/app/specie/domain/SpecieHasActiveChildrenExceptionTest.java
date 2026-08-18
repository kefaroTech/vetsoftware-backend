package com.vetsoftware.app.specie.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpecieHasActiveChildrenException")
class SpecieHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje identifica la especie y el tipo de hijo que bloquea el borrado")
    void el_mensaje_identifica_la_especie_y_el_tipo_de_hijo() {
        SpecieHasActiveChildrenException exception = new SpecieHasActiveChildrenException(7L,
                "breed");

        assertThat(exception.getMessage()).contains("7").contains("breed");
    }

    @Test
    @DisplayName("distintos tipos de hijo producen mensajes distintos")
    void distintos_tipos_de_hijo_producen_mensajes_distintos() {
        SpecieHasActiveChildrenException porAnimal = new SpecieHasActiveChildrenException(7L,
                "animal");

        assertThat(porAnimal.getMessage()).contains("animal").doesNotContain("breed");
    }
}
