package com.vetsoftware.app.tax.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TaxNotFoundException")
class TaxNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del impuesto")
    void el_mensaje_incluye_el_id() {
        assertThat(new TaxNotFoundException(500L)).hasMessageContaining("Tax not found: 500");
    }
}
