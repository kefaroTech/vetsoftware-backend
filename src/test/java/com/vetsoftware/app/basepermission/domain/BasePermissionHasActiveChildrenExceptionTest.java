package com.vetsoftware.app.basepermission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BasePermissionHasActiveChildrenException — mensaje de dominio")
class BasePermissionHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del permiso base y el tipo de hijo que lo bloquea")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        BasePermissionHasActiveChildrenException ex = new BasePermissionHasActiveChildrenException(
                5L, "baseRolePermission");

        assertThat(ex.getMessage()).contains("5").contains("baseRolePermission");
    }
}
