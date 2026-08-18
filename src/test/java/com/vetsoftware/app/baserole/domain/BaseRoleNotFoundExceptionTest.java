package com.vetsoftware.app.baserole.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BaseRoleNotFoundException")
class BaseRoleNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id buscado")
    void el_mensaje_incluye_el_id_buscado() {
        BaseRoleNotFoundException exception = new BaseRoleNotFoundException(9L);

        assertThat(exception.getMessage()).contains("9");
    }
}
