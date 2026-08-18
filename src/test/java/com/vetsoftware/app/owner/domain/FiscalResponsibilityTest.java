package com.vetsoftware.app.owner.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FiscalResponsibility.defaultValue — responsabilidad fiscal por defecto")
class FiscalResponsibilityTest {

    @Test
    @DisplayName("el valor por defecto es NO_APLICA (R-99-PN)")
    void el_valor_por_defecto_es_no_aplica() {
        assertThat(FiscalResponsibility.defaultValue()).isEqualTo(FiscalResponsibility.NO_APLICA);
    }
}
