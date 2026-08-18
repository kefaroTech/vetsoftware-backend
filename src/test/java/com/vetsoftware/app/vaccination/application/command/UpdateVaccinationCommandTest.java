package com.vetsoftware.app.vaccination.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateVaccinationCommand")
class UpdateVaccinationCommandTest {

    @Test
    @DisplayName("expone cada campo tal como se construyo, incluido el id")
    void expone_cada_campo_tal_como_se_construyo() {
        LocalDate fecha = LocalDate.of(2026, 3, 1);
        LocalDate proxima = LocalDate.of(2027, 3, 1);

        UpdateVaccinationCommand command = new UpdateVaccinationCommand(100L, fecha, 11L,
                "L-2026-B", "Reaccion leve", "Intramuscular", "Muslo", proxima, 12L, 13L, 9L);

        assertThat(command.id()).isEqualTo(100L);
        assertThat(command.date()).isEqualTo(fecha);
        assertThat(command.vaccinationTypeId()).isEqualTo(11L);
        assertThat(command.lot()).isEqualTo("L-2026-B");
        assertThat(command.notes()).isEqualTo("Reaccion leve");
        assertThat(command.route()).isEqualTo("Intramuscular");
        assertThat(command.applicationSite()).isEqualTo("Muslo");
        assertThat(command.nextVaccination()).isEqualTo(proxima);
        assertThat(command.animalId()).isEqualTo(12L);
        assertThat(command.consultationId()).isEqualTo(13L);
        assertThat(command.companyId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("dos comandos con los mismos valores son iguales — contrato de record")
    void dos_comandos_con_los_mismos_valores_son_iguales() {
        LocalDate fecha = LocalDate.of(2026, 3, 1);

        UpdateVaccinationCommand uno = new UpdateVaccinationCommand(100L, fecha, 11L, "L-2026-B",
                null, null, null, null, 12L, null, 9L);
        UpdateVaccinationCommand otro = new UpdateVaccinationCommand(100L, fecha, 11L, "L-2026-B",
                null, null, null, null, 12L, null, 9L);

        assertThat(uno).isEqualTo(otro);
    }
}
