package com.vetsoftware.app.problem.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProblemNotFoundException")
class ProblemNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del problema no encontrado")
    void el_mensaje_incluye_el_id() {
        ProblemNotFoundException exception = new ProblemNotFoundException(200L);

        assertThat(exception.getMessage()).isEqualTo("Problem not found: 200");
    }
}
