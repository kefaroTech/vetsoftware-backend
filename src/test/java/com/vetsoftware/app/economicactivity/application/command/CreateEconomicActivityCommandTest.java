package com.vetsoftware.app.economicactivity.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateEconomicActivityCommand")
class CreateEconomicActivityCommandTest {

    @Test
    @DisplayName("conserva codigo y nombre")
    void conserva_codigo_y_nombre() {
        CreateEconomicActivityCommand command = new CreateEconomicActivityCommand("0111",
                "Cultivo de cereales");

        assertThat(command.code()).isEqualTo("0111");
        assertThat(command.name()).isEqualTo("Cultivo de cereales");
    }
}
