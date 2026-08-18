package com.vetsoftware.app.systemuser.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemUserNotFoundException")
class SystemUserNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id buscado")
    void el_mensaje_incluye_el_id_buscado() {
        SystemUserNotFoundException exception = new SystemUserNotFoundException(100L);

        assertThat(exception.getMessage()).contains("SystemUser not found: 100");
    }
}
