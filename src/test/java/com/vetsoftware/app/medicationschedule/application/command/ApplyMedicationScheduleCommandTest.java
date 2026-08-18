package com.vetsoftware.app.medicationschedule.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ApplyMedicationScheduleCommand")
class ApplyMedicationScheduleCommandTest {

    @Test
    @DisplayName("expone el id de la toma y la empresa que la aplica")
    void expone_el_id_de_la_toma_y_la_empresa() {
        ApplyMedicationScheduleCommand command = new ApplyMedicationScheduleCommand(500L, 9L);

        assertThat(command.scheduleId()).isEqualTo(500L);
        assertThat(command.companyId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("sin empresa representa el camino SYSTEM")
    void sin_empresa_representa_el_camino_system() {
        ApplyMedicationScheduleCommand command = new ApplyMedicationScheduleCommand(500L, null);

        assertThat(command.companyId()).isNull();
    }
}
