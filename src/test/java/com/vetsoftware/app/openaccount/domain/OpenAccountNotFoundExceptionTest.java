package com.vetsoftware.app.openaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OpenAccountNotFoundException")
class OpenAccountNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id buscado")
    void el_mensaje_incluye_el_id_buscado() {
        OpenAccountNotFoundException ex = new OpenAccountNotFoundException(77L);

        assertThat(ex.getMessage()).contains("OpenAccount not found").contains("77");
    }
}
