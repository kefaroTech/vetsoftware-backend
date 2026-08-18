package com.vetsoftware.app.numberingresolution.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NumberingResolutionAlreadyActiveException")
class NumberingResolutionAlreadyActiveExceptionTest {

    @Test
    @DisplayName("el mensaje incluye la empresa y el tipo de documento en conflicto")
    void el_mensaje_incluye_la_empresa_y_el_tipo_de_documento() {
        NumberingResolutionAlreadyActiveException ex = new NumberingResolutionAlreadyActiveException(
                9L, ElectronicDocumentType.FE_VENTA);

        assertThat(ex.getMessage()).contains("9").contains("FE_VENTA")
                .contains("ya tiene una resolución de numeración activa");
    }
}
