package com.vetsoftware.app.consultationtype.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateConsultationTypeCommand")
class CreateConsultationTypeCommandTest {

    @Test
    @DisplayName("conserva name y description tal cual se le pasan")
    void conserva_name_y_description() {
        CreateConsultationTypeCommand command = new CreateConsultationTypeCommand("Vacunacion",
                "Aplicacion de vacunas");

        assertThat(command.name()).isEqualTo("Vacunacion");
        assertThat(command.description()).isEqualTo("Aplicacion de vacunas");
    }
}
