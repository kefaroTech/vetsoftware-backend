package com.vetsoftware.app.systemuserpermission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemUserPermissionNotFoundException")
class SystemUserPermissionNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id que no se encontro")
    void el_mensaje_incluye_el_id_que_no_se_encontro() {
        SystemUserPermissionNotFoundException exception = new SystemUserPermissionNotFoundException(
                42L);

        assertThat(exception).hasMessageContaining("42")
                .hasMessageContaining("SystemUserPermission not found");
    }
}
