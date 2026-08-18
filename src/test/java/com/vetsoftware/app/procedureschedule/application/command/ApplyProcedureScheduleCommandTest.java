package com.vetsoftware.app.procedureschedule.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ApplyProcedureScheduleCommand")
class ApplyProcedureScheduleCommandTest {

    @Test
    @DisplayName("expone el id de la toma y la empresa que la aplica")
    void expone_el_id_de_la_toma_y_la_empresa() {
        ApplyProcedureScheduleCommand command = new ApplyProcedureScheduleCommand(500L, 9L);

        assertThat(command.scheduleId()).isEqualTo(500L);
        assertThat(command.companyId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("sin empresa representa el camino SYSTEM")
    void sin_empresa_representa_el_camino_system() {
        ApplyProcedureScheduleCommand command = new ApplyProcedureScheduleCommand(500L, null);

        assertThat(command.companyId()).isNull();
    }
}
