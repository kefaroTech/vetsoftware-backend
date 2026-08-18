package com.vetsoftware.app.systemuser.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateSystemUserCommand")
class CreateSystemUserCommandTest {

    @Test
    @DisplayName("expone code y password tal como se construyo")
    void expone_code_y_password_tal_como_se_construyo() {
        CreateSystemUserCommand command = new CreateSystemUserCommand("svc-integracion",
                "unaContrasenaSegura1");

        assertThat(command.code()).isEqualTo("svc-integracion");
        assertThat(command.password()).isEqualTo("unaContrasenaSegura1");
    }
}
