package com.vetsoftware.app.submodule.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubModuleNotFoundException — mensaje de dominio")
class SubModuleNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del submodulo no encontrado")
    void el_mensaje_incluye_el_id_no_encontrado() {
        SubModuleNotFoundException ex = new SubModuleNotFoundException(42L);

        assertThat(ex.getMessage()).contains("SubModule not found").contains("42");
    }
}
