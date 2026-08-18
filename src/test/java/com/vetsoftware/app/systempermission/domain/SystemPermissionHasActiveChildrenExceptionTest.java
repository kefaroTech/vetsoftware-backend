package com.vetsoftware.app.systempermission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemPermissionHasActiveChildrenException")
class SystemPermissionHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del padre y el tipo de hijo activo")
    void el_mensaje_incluye_id_y_tipo_de_hijo() {
        assertThat(new SystemPermissionHasActiveChildrenException(7L, "systemUserPermission"))
                .hasMessageContaining("7").hasMessageContaining("systemUserPermission");
    }
}
