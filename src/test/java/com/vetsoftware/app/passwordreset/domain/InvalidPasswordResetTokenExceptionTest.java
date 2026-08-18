package com.vetsoftware.app.passwordreset.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidPasswordResetTokenException")
class InvalidPasswordResetTokenExceptionTest {

    @Test
    @DisplayName("conserva el mensaje y es una RuntimeException")
    void conserva_el_mensaje_y_es_runtime_exception() {
        InvalidPasswordResetTokenException exception = new InvalidPasswordResetTokenException(
                "Password reset token expired");

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo("Password reset token expired");
    }
}
