package com.vetsoftware.app.surgerytype.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateSurgeryTypeCommand")
class CreateSurgeryTypeCommandTest {

    @Test
    @DisplayName("conserva cada campo tal cual se recibe")
    void conserva_cada_campo() {
        CreateSurgeryTypeCommand command = new CreateSurgeryTypeCommand("Castracion",
                "Cirugia de esterilizacion", 9L, false);

        assertThat(command.name()).isEqualTo("Castracion");
        assertThat(command.description()).isEqualTo("Cirugia de esterilizacion");
        assertThat(command.companyId()).isEqualTo(9L);
        assertThat(command.general()).isFalse();
    }

    @Test
    @DisplayName("un comando general puede llevar companyId nulo")
    void un_comando_general_puede_llevar_company_id_nulo() {
        CreateSurgeryTypeCommand command = new CreateSurgeryTypeCommand("Cirugia general",
                "Procedimiento estandar", null, true);

        assertThat(command.companyId()).isNull();
        assertThat(command.general()).isTrue();
    }
}
