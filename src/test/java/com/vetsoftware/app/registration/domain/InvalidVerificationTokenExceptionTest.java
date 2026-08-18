package com.vetsoftware.app.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidVerificationTokenException")
class InvalidVerificationTokenExceptionTest {

    @Test
    @DisplayName("expone el mensaje recibido")
    void expone_el_mensaje_recibido() {
        InvalidVerificationTokenException exception = new InvalidVerificationTokenException(
                "Verification token expired");

        assertThat(exception.getMessage()).isEqualTo("Verification token expired");
    }
}
