package com.vetsoftware.app.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionNotFoundException")
class PermissionNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id buscado")
    void el_mensaje_incluye_el_id_buscado() {
        PermissionNotFoundException exception = new PermissionNotFoundException(42L);

        assertThat(exception.getMessage()).contains("42");
    }
}
