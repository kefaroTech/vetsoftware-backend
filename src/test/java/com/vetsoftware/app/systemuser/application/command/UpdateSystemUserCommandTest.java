package com.vetsoftware.app.systemuser.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateSystemUserCommand")
class UpdateSystemUserCommandTest {

    @Test
    @DisplayName("expone id y code tal como se construyo")
    void expone_id_y_code_tal_como_se_construyo() {
        UpdateSystemUserCommand command = new UpdateSystemUserCommand(100L, "svc-nuevo");

        assertThat(command.id()).isEqualTo(100L);
        assertThat(command.code()).isEqualTo("svc-nuevo");
    }
}
