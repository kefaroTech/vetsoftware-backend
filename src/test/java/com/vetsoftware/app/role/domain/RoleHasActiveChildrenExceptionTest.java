package com.vetsoftware.app.role.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RoleHasActiveChildrenException")
class RoleHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del rol y el tipo de hijo activo")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        var exception = new RoleHasActiveChildrenException(7L, "employeeRole");

        assertThat(exception.getMessage()).contains("7").contains("employeeRole");
    }
}
