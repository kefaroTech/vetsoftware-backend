package com.vetsoftware.app.diagnosticimagingtype.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DiagnosticImagingTypeNameAlreadyExistsException")
class DiagnosticImagingTypeNameAlreadyExistsExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el nombre repetido")
    void el_mensaje_incluye_el_nombre_repetido() {
        DiagnosticImagingTypeNameAlreadyExistsException ex = new DiagnosticImagingTypeNameAlreadyExistsException(
                "Radiografia");

        assertThat(ex.getMessage()).contains("Radiografia");
    }

    @Test
    @DisplayName("el mensaje va en espanol y nombra el ambito, que es lo que el 409 generico no decia")
    void el_mensaje_va_en_espanol_y_nombra_el_ambito() {
        // Antes de #559 el choque salia como "Database constraint violation": en
        // ingles, sin nombrar el campo, asi que el formulario no podia marcar `name`.
        DiagnosticImagingTypeNameAlreadyExistsException ex = new DiagnosticImagingTypeNameAlreadyExistsException(
                "Radiografia");

        assertThat(ex.getMessage()).contains("Ya existe un tipo de imagen diagnóstica activo")
                .contains("en este ámbito");
    }
}
