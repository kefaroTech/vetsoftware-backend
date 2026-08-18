package com.vetsoftware.app.procedureschedule.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RescheduleProcedureScheduleCommand")
class RescheduleProcedureScheduleCommandTest {

    @Test
    @DisplayName("expone la toma, la nueva hora, el modo y la empresa que reprograma")
    void expone_la_toma_la_hora_el_modo_y_la_empresa() {
        LocalDateTime nueva = LocalDateTime.of(2026, 1, 16, 8, 0);

        RescheduleProcedureScheduleCommand command = new RescheduleProcedureScheduleCommand(500L,
                nueva, "cascade", 9L);

        assertThat(command.scheduleId()).isEqualTo(500L);
        assertThat(command.newDateTime()).isEqualTo(nueva);
        assertThat(command.mode()).isEqualTo("cascade");
        assertThat(command.companyId()).isEqualTo(9L);
    }
}
