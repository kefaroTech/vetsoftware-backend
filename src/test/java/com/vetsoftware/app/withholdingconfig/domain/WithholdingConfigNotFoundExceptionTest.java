package com.vetsoftware.app.withholdingconfig.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WithholdingConfigNotFoundException")
class WithholdingConfigNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje identifica la company sin configuracion")
    void el_mensaje_identifica_la_company() {
        WithholdingConfigNotFoundException exception = new WithholdingConfigNotFoundException(42L);

        assertThat(exception.getMessage()).contains("42").contains("Withholding config not found");
    }
}
