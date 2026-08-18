package com.vetsoftware.app.membership.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateMembershipCommand")
class UpdateMembershipCommandTest {

    @Test
    @DisplayName("conserva cada campo en su posicion, incluido el id")
    void conserva_cada_campo_en_su_posicion_incluido_el_id() {
        UpdateMembershipCommand command = new UpdateMembershipCommand(100L, "Plan Platino",
                "DEPRECATED", true);

        assertThat(command.id()).isEqualTo(100L);
        assertThat(command.name()).isEqualTo("Plan Platino");
        assertThat(command.status()).isEqualTo("DEPRECATED");
        assertThat(command.mandatory()).isTrue();
    }
}
