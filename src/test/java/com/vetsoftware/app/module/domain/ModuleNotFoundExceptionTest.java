package com.vetsoftware.app.module.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ModuleNotFoundException")
class ModuleNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id que no se encontro")
    void el_mensaje_incluye_el_id() {
        assertThat(new ModuleNotFoundException(42L)).hasMessageContaining("42");
    }
}
