package com.vetsoftware.app.consultationtype.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateConsultationTypeCommand")
class UpdateConsultationTypeCommandTest {

    @Test
    @DisplayName("conserva id, name y description tal cual se le pasan")
    void conserva_id_name_y_description() {
        UpdateConsultationTypeCommand command = new UpdateConsultationTypeCommand(9L, "Vacunacion",
                "Aplicacion de vacunas");

        assertThat(command.id()).isEqualTo(9L);
        assertThat(command.name()).isEqualTo("Vacunacion");
        assertThat(command.description()).isEqualTo("Aplicacion de vacunas");
    }
}
