package com.vetsoftware.app.openaccount.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateOpenAccountCommand")
class UpdateOpenAccountCommandTest {

    @Test
    @DisplayName("conserva cada campo")
    void conserva_cada_campo() {
        UpdateOpenAccountCommand command = new UpdateOpenAccountCommand(100L, 2L, 9L, 3L);

        assertThat(command.id()).isEqualTo(100L);
        assertThat(command.ownerId()).isEqualTo(2L);
        assertThat(command.companyId()).isEqualTo(9L);
        assertThat(command.expectedVersion()).isEqualTo(3L);
    }

    @Test
    @DisplayName("admite expectedVersion null (sin chequeo de version)")
    void admite_expected_version_null() {
        UpdateOpenAccountCommand command = new UpdateOpenAccountCommand(100L, 2L, 9L, null);

        assertThat(command.expectedVersion()).isNull();
    }
}
