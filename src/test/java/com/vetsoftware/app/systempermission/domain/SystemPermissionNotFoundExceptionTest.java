package com.vetsoftware.app.systempermission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemPermissionNotFoundException")
class SystemPermissionNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id que no se encontro")
    void el_mensaje_incluye_el_id() {
        assertThat(new SystemPermissionNotFoundException(42L)).hasMessageContaining("42");
    }
}
