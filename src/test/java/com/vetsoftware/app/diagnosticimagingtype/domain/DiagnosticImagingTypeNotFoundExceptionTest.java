package com.vetsoftware.app.diagnosticimagingtype.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DiagnosticImagingTypeNotFoundException")
class DiagnosticImagingTypeNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id buscado")
    void el_mensaje_incluye_el_id_buscado() {
        DiagnosticImagingTypeNotFoundException exception = new DiagnosticImagingTypeNotFoundException(
                42L);

        assertThat(exception.getMessage()).contains("42");
    }
}
