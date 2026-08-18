package com.vetsoftware.app.diagnosticimagingtype.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DiagnosticImagingTypeHasActiveChildrenException")
class DiagnosticImagingTypeHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id y el tipo de hijo activo")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        DiagnosticImagingTypeHasActiveChildrenException exception = new DiagnosticImagingTypeHasActiveChildrenException(
                7L, "diagnosticImaging");

        assertThat(exception.getMessage()).contains("7").contains("diagnosticImaging");
    }
}
