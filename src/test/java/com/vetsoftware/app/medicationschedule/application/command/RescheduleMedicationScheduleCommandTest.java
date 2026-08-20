package com.vetsoftware.app.medicationschedule.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.medicationschedule.domain.RescheduleMode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("RescheduleMedicationScheduleCommand")
class RescheduleMedicationScheduleCommandTest {

    private static final LocalDateTime NUEVA = LocalDateTime.of(2026, 1, 16, 8, 0);

    @Test
    @DisplayName("expone la toma, la nueva hora, el modo y la empresa que reprograma")
    void expone_la_toma_la_hora_el_modo_y_la_empresa() {
        RescheduleMedicationScheduleCommand command = new RescheduleMedicationScheduleCommand(500L,
                NUEVA, RescheduleMode.CASCADE, 9L);

        assertThat(command.scheduleId()).isEqualTo(500L);
        assertThat(command.newDateTime()).isEqualTo(NUEVA);
        assertThat(command.mode()).isEqualTo(RescheduleMode.CASCADE);
        assertThat(command.companyId()).isEqualTo(9L);
    }

    /**
     * El modo era {@code String} libre y el caso de uso lo comparaba con
     * {@code "cascade".equalsIgnoreCase(...)}: todo lo demas degradaba a «solo esta
     * toma» en silencio (#134). Recorrer el enum entero deja el command atado a los
     * valores que existen, y obliga a pasar por aqui si aparece uno nuevo.
     */
    @ParameterizedTest(name = "modo {0}")
    @EnumSource(RescheduleMode.class)
    @DisplayName("transporta cada modo tal cual, sin traducirlo ni asumir uno por defecto")
    void transporta_cada_modo_tal_cual(RescheduleMode mode) {
        RescheduleMedicationScheduleCommand command = new RescheduleMedicationScheduleCommand(500L,
                NUEVA, mode, 9L);

        assertThat(command.mode()).isSameAs(mode);
    }

    @Test
    @DisplayName("acepta empresa nula, que es el camino SYSTEM")
    void acepta_empresa_nula() {
        RescheduleMedicationScheduleCommand command = new RescheduleMedicationScheduleCommand(500L,
                NUEVA, RescheduleMode.ONE, null);

        assertThat(command.companyId()).isNull();
    }
}
