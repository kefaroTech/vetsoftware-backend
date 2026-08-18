package com.vetsoftware.app.economicactivity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EconomicActivityNotFoundException")
class EconomicActivityNotFoundExceptionTest {

    @Test
    @DisplayName("lleva el id en el mensaje para que el 404 diga cual")
    void lleva_el_id_en_el_mensaje() {
        assertThat(new EconomicActivityNotFoundException(77L))
                .hasMessageContaining("EconomicActivity not found: 77");
    }
}
