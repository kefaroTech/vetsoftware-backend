package com.vetsoftware.app.baserole.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BaseRoleHasActiveChildrenException")
class BaseRoleHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id y el tipo de hijo activo")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo_activo() {
        BaseRoleHasActiveChildrenException exception = new BaseRoleHasActiveChildrenException(5L,
                "baseRolePermission");

        assertThat(exception.getMessage()).contains("5").contains("baseRolePermission");
    }
}
