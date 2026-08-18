package com.vetsoftware.app.vaccinationtype.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateVaccinationTypeCommand")
class UpdateVaccinationTypeCommandTest {

    @Test
    @DisplayName("conserva cada campo en su posicion")
    void conserva_cada_campo_en_su_posicion() {
        UpdateVaccinationTypeCommand command = new UpdateVaccinationTypeCommand(50L, "Moquillo",
                "Vacuna contra el moquillo", 9L, false);

        assertThat(command.id()).isEqualTo(50L);
        assertThat(command.name()).isEqualTo("Moquillo");
        assertThat(command.description()).isEqualTo("Vacuna contra el moquillo");
        assertThat(command.companyId()).isEqualTo(9L);
        assertThat(command.general()).isFalse();
    }

    @Test
    @DisplayName("un tipo general puede llevar companyId nulo")
    void un_tipo_general_puede_llevar_company_id_nulo() {
        UpdateVaccinationTypeCommand command = new UpdateVaccinationTypeCommand(50L,
                "Vacuna universal", "Disponible para todas", null, true);

        assertThat(command.companyId()).isNull();
        assertThat(command.general()).isTrue();
    }
}
