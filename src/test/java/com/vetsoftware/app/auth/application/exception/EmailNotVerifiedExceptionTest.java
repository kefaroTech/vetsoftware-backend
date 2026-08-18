package com.vetsoftware.app.auth.application.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailNotVerifiedExceptionTest {

    @Test
    @DisplayName("expone el código de empleado intentado y un mensaje estable")
    void expone_el_codigo_y_un_mensaje_estable() {
        EmailNotVerifiedException exception = new EmailNotVerifiedException("EMP-1");

        assertThat(exception.getIdentifier()).isEqualTo("EMP-1");
        assertThat(exception.getMessage()).isEqualTo("Email not verified");
    }
}
