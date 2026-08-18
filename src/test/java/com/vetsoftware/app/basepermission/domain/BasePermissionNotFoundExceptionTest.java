package com.vetsoftware.app.basepermission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BasePermissionNotFoundException — mensaje de dominio")
class BasePermissionNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del permiso base no encontrado")
    void el_mensaje_incluye_el_id_no_encontrado() {
        BasePermissionNotFoundException ex = new BasePermissionNotFoundException(42L);

        assertThat(ex.getMessage()).contains("BasePermission not found").contains("42");
    }
}
