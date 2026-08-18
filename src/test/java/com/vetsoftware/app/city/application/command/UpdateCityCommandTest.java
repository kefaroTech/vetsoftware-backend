package com.vetsoftware.app.city.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateCityCommand — portador de datos")
class UpdateCityCommandTest {

    @Test
    @DisplayName("conserva cada campo en su sitio")
    void conserva_cada_campo_en_su_sitio() {
        UpdateCityCommand command = new UpdateCityCommand(80L, "Envigado", 9L, "05266");

        assertThat(command.id()).isEqualTo(80L);
        assertThat(command.name()).isEqualTo("Envigado");
        assertThat(command.stateId()).isEqualTo(9L);
        assertThat(command.daneCode()).isEqualTo("05266");
    }
}
