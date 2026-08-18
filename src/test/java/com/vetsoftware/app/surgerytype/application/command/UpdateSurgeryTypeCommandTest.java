package com.vetsoftware.app.surgerytype.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateSurgeryTypeCommand")
class UpdateSurgeryTypeCommandTest {

    @Test
    @DisplayName("conserva cada campo tal cual se recibe, incluido el id de la ruta")
    void conserva_cada_campo() {
        UpdateSurgeryTypeCommand command = new UpdateSurgeryTypeCommand(700L, "Castracion avanzada",
                "Nueva descripcion", 9L, false);

        assertThat(command.id()).isEqualTo(700L);
        assertThat(command.name()).isEqualTo("Castracion avanzada");
        assertThat(command.description()).isEqualTo("Nueva descripcion");
        assertThat(command.companyId()).isEqualTo(9L);
        assertThat(command.general()).isFalse();
    }
}
