package com.vetsoftware.app.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionHasActiveChildrenException")
class PermissionHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id y el tipo de hijo activo")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        PermissionHasActiveChildrenException exception = new PermissionHasActiveChildrenException(
                7L, "rolePermission");

        assertThat(exception.getMessage()).contains("7").contains("rolePermission");
    }
}
