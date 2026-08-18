package com.vetsoftware.app.economicactivity.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateEconomicActivityCommand")
class UpdateEconomicActivityCommandTest {

    @Test
    @DisplayName("conserva id, codigo y nombre")
    void conserva_id_codigo_y_nombre() {
        UpdateEconomicActivityCommand command = new UpdateEconomicActivityCommand(70L, "0112",
                "Cultivo de hortalizas");

        assertThat(command.id()).isEqualTo(70L);
        assertThat(command.code()).isEqualTo("0112");
        assertThat(command.name()).isEqualTo("Cultivo de hortalizas");
    }
}
