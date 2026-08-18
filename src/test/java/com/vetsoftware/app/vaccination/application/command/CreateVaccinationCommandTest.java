package com.vetsoftware.app.vaccination.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateVaccinationCommand")
class CreateVaccinationCommandTest {

    @Test
    @DisplayName("expone cada campo tal como se construyo")
    void expone_cada_campo_tal_como_se_construyo() {
        LocalDate fecha = LocalDate.of(2026, 1, 15);
        LocalDate proxima = LocalDate.of(2027, 1, 15);

        CreateVaccinationCommand command = new CreateVaccinationCommand(fecha, 1L, "L-2026-A",
                "Sin reaccion", "Subcutanea", "Cuello", proxima, 2L, 3L, 9L);

        assertThat(command.date()).isEqualTo(fecha);
        assertThat(command.vaccinationTypeId()).isEqualTo(1L);
        assertThat(command.lot()).isEqualTo("L-2026-A");
        assertThat(command.notes()).isEqualTo("Sin reaccion");
        assertThat(command.route()).isEqualTo("Subcutanea");
        assertThat(command.applicationSite()).isEqualTo("Cuello");
        assertThat(command.nextVaccination()).isEqualTo(proxima);
        assertThat(command.animalId()).isEqualTo(2L);
        assertThat(command.consultationId()).isEqualTo(3L);
        assertThat(command.companyId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("dos comandos con los mismos valores son iguales — contrato de record")
    void dos_comandos_con_los_mismos_valores_son_iguales() {
        LocalDate fecha = LocalDate.of(2026, 1, 15);

        CreateVaccinationCommand uno = new CreateVaccinationCommand(fecha, 1L, "L-2026-A", null,
                null, null, null, 2L, null, 9L);
        CreateVaccinationCommand otro = new CreateVaccinationCommand(fecha, 1L, "L-2026-A", null,
                null, null, null, 2L, null, 9L);

        assertThat(uno).isEqualTo(otro);
    }
}
