package com.vetsoftware.app.laboratorytesttype.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LaboratoryTestTypeNameAlreadyExistsException")
class LaboratoryTestTypeNameAlreadyExistsExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el nombre repetido")
    void el_mensaje_incluye_el_nombre_repetido() {
        LaboratoryTestTypeNameAlreadyExistsException ex = new LaboratoryTestTypeNameAlreadyExistsException(
                "Hemograma");

        assertThat(ex.getMessage()).contains("Hemograma");
    }

    @Test
    @DisplayName("el mensaje va en espanol y nombra el ambito, que es lo que el 409 generico no decia")
    void el_mensaje_va_en_espanol_y_nombra_el_ambito() {
        // Antes de #559 el choque salia como "Database constraint violation": en
        // ingles, sin nombrar el campo, asi que el formulario no podia marcar `name`.
        LaboratoryTestTypeNameAlreadyExistsException ex = new LaboratoryTestTypeNameAlreadyExistsException(
                "Hemograma");

        assertThat(ex.getMessage()).contains("Ya existe un tipo de examen de laboratorio activo")
                .contains("en este ámbito");
    }
}
