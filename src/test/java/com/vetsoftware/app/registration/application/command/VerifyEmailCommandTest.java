package com.vetsoftware.app.registration.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VerifyEmailCommand")
class VerifyEmailCommandTest {

    @Test
    @DisplayName("expone el token recibido")
    void expone_el_token_recibido() {
        VerifyEmailCommand command = new VerifyEmailCommand("raw-token-value");

        assertThat(command.token()).isEqualTo("raw-token-value");
    }
}
