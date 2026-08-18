package com.vetsoftware.app.membership.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateMembershipCommand")
class CreateMembershipCommandTest {

    @Test
    @DisplayName("conserva cada campo en su posicion")
    void conserva_cada_campo_en_su_posicion() {
        CreateMembershipCommand command = new CreateMembershipCommand("Plan Oro", "ACTIVE", true);

        assertThat(command.name()).isEqualTo("Plan Oro");
        assertThat(command.status()).isEqualTo("ACTIVE");
        assertThat(command.mandatory()).isTrue();
    }
}
