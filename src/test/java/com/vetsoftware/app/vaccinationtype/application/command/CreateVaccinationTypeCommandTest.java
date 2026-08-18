package com.vetsoftware.app.vaccinationtype.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateVaccinationTypeCommand")
class CreateVaccinationTypeCommandTest {

    @Test
    @DisplayName("conserva cada campo en su posicion")
    void conserva_cada_campo_en_su_posicion() {
        CreateVaccinationTypeCommand command = new CreateVaccinationTypeCommand("Rabia",
                "Vacuna antirrabica", 9L, false);

        assertThat(command.name()).isEqualTo("Rabia");
        assertThat(command.description()).isEqualTo("Vacuna antirrabica");
        assertThat(command.companyId()).isEqualTo(9L);
        assertThat(command.general()).isFalse();
    }

    @Test
    @DisplayName("un tipo general puede llevar companyId nulo")
    void un_tipo_general_puede_llevar_company_id_nulo() {
        CreateVaccinationTypeCommand command = new CreateVaccinationTypeCommand("Vacuna universal",
                "Disponible para todas", null, true);

        assertThat(command.companyId()).isNull();
        assertThat(command.general()).isTrue();
    }
}
