package com.vetsoftware.app.animalalert.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalAlertNotFoundException")
class AnimalAlertNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id de la alerta que no se encontro")
    void el_mensaje_incluye_el_id() {
        AnimalAlertNotFoundException exception = new AnimalAlertNotFoundException(500L);

        assertThat(exception.getMessage()).contains("AnimalAlert not found: 500");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
