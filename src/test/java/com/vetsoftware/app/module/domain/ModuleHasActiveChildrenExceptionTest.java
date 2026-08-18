package com.vetsoftware.app.module.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ModuleHasActiveChildrenException")
class ModuleHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del padre y el tipo de hijo activo")
    void el_mensaje_incluye_id_y_tipo_de_hijo() {
        assertThat(new ModuleHasActiveChildrenException(7L, "subModule")).hasMessageContaining("7")
                .hasMessageContaining("subModule");
    }
}
