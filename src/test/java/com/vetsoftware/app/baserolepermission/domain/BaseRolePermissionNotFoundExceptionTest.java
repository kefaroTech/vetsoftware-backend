package com.vetsoftware.app.baserolepermission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BaseRolePermissionNotFoundException — mensaje de dominio")
class BaseRolePermissionNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del vinculo no encontrado")
    void el_mensaje_incluye_el_id_no_encontrado() {
        BaseRolePermissionNotFoundException ex = new BaseRolePermissionNotFoundException(42L);

        assertThat(ex.getMessage()).contains("BaseRolePermission not found").contains("42");
    }
}
