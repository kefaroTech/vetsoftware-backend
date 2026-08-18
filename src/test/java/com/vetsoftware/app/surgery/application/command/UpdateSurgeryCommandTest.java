package com.vetsoftware.app.surgery.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.surgery.testsupport.SurgeryMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateSurgeryCommand")
class UpdateSurgeryCommandTest {

    @Test
    @DisplayName("el constructor canonico conserva cada campo, incluido el id de la ruta")
    void el_constructor_canonico_conserva_cada_campo() {
        UpdateSurgeryCommand command = SurgeryMother.comandoActualizar();

        assertThat(command.id()).isEqualTo(SurgeryMother.SURGERY_ID);
        assertThat(command.date()).isEqualTo(SurgeryMother.FECHA.plusDays(5));
        assertThat(command.surgeryTypeId()).isEqualTo(SurgeryMother.CASTRACION.id());
        assertThat(command.description()).isEqualTo("Castracion electiva");
        assertThat(command.medicament()).isEqualTo("Anestesia local");
        assertThat(command.observations()).isEqualTo("Observaciones nuevas");
        assertThat(command.complications()).isEqualTo("Sangrado leve");
        assertThat(command.animalId()).isEqualTo(SurgeryMother.MICHI.id());
        assertThat(command.consultationId()).isEqualTo(SurgeryMother.OTRA_CONSULTA.id());
        assertThat(command.companyId()).isEqualTo(SurgeryMother.COMPANY_ID);
    }
}
