package com.vetsoftware.app.systemuser.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemUserHasActiveChildrenException")
class SystemUserHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id y el tipo de hijo activo")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        SystemUserHasActiveChildrenException exception = new SystemUserHasActiveChildrenException(
                100L, "systemUserPermission");

        assertThat(exception.getMessage()).contains("Cannot delete systemuser 100")
                .contains("has active systemUserPermission children");
    }
}
