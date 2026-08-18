package com.vetsoftware.app.city.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateCityCommand — portador de datos")
class CreateCityCommandTest {

    @Test
    @DisplayName("conserva cada campo en su sitio")
    void conserva_cada_campo_en_su_sitio() {
        CreateCityCommand command = new CreateCityCommand("Medellin", 9L, "05001");

        assertThat(command.name()).isEqualTo("Medellin");
        assertThat(command.stateId()).isEqualTo(9L);
        assertThat(command.daneCode()).isEqualTo("05001");
    }
}
