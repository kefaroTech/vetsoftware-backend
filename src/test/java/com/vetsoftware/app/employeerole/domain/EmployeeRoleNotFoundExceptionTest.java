package com.vetsoftware.app.employeerole.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmployeeRoleNotFoundException")
class EmployeeRoleNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id que no se encontro")
    void el_mensaje_incluye_el_id() {
        EmployeeRoleNotFoundException exception = new EmployeeRoleNotFoundException(500L);

        assertThat(exception.getMessage()).contains("500");
    }
}
