package com.vetsoftware.app.tax.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TaxHasActiveChildrenException")
class TaxHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del impuesto y el tipo de hijo")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        assertThat(new TaxHasActiveChildrenException(500L, "product/service"))
                .hasMessageContaining("500").hasMessageContaining("product/service");
    }
}
