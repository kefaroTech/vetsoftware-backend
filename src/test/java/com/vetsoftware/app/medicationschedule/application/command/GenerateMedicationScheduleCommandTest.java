package com.vetsoftware.app.medicationschedule.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GenerateMedicationScheduleCommand")
class GenerateMedicationScheduleCommandTest {

    @Test
    @DisplayName("expone la orden a agendar y el empleado que la dispara")
    void expone_la_orden_y_el_empleado() {
        GenerateMedicationScheduleCommand command = new GenerateMedicationScheduleCommand(300L, 7L,
                9L);

        assertThat(command.hospitalizationMedicationId()).isEqualTo(300L);
        assertThat(command.createdById()).isEqualTo(7L);
        assertThat(command.companyId()).isEqualTo(9L);
    }
}
