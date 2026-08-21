package com.vetsoftware.app.hospitalizationprogressnote.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateHospitalizationProgressNoteCommand")
class CreateHospitalizationProgressNoteCommandTest {

    @Test
    @DisplayName("expone description, hospitalizationId, createdById y companyId tal como se construyo")
    void expone_cada_campo() {
        CreateHospitalizationProgressNoteCommand command = new CreateHospitalizationProgressNoteCommand(
                "Evolucion favorable", 55L, 4L, 9L);

        assertThat(command.description()).isEqualTo("Evolucion favorable");
        assertThat(command.hospitalizationId()).isEqualTo(55L);
        assertThat(command.createdById()).isEqualTo(4L);
        assertThat(command.companyId()).isEqualTo(9L);
    }
}
