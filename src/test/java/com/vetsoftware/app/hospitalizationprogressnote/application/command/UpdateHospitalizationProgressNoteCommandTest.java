package com.vetsoftware.app.hospitalizationprogressnote.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateHospitalizationProgressNoteCommand")
class UpdateHospitalizationProgressNoteCommandTest {

    @Test
    @DisplayName("expone id, description y la empresa tal como se construyo")
    void expone_cada_campo() {
        UpdateHospitalizationProgressNoteCommand command = new UpdateHospitalizationProgressNoteCommand(
                500L, "Evolucion favorable", 9L);

        assertThat(command.id()).isEqualTo(500L);
        assertThat(command.description()).isEqualTo("Evolucion favorable");
        assertThat(command.companyId()).isEqualTo(9L);
    }
}
