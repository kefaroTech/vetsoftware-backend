package com.vetsoftware.app.procedureschedule.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GenerateProcedureScheduleCommand")
class GenerateProcedureScheduleCommandTest {

    @Test
    @DisplayName("expone la orden a agendar y el empleado que la dispara")
    void expone_la_orden_y_el_empleado() {
        GenerateProcedureScheduleCommand command = new GenerateProcedureScheduleCommand(300L, 7L,
                9L);

        assertThat(command.hospitalizationProcedureId()).isEqualTo(300L);
        assertThat(command.createdById()).isEqualTo(7L);
        assertThat(command.companyId()).isEqualTo(9L);
    }
}
