package com.vetsoftware.app.tax.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TaxNameAlreadyExistsException")
class TaxNameAlreadyExistsExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el nombre repetido")
    void el_mensaje_incluye_el_nombre() {
        assertThat(new TaxNameAlreadyExistsException("IVA General"))
                .hasMessageContaining("IVA General");
    }
}
