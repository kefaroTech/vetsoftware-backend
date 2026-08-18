package com.vetsoftware.app.surgery.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.surgery.testsupport.SurgeryMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateSurgeryCommand")
class CreateSurgeryCommandTest {

    @Test
    @DisplayName("el constructor canonico conserva cada campo, incluida la consulta opcional")
    void el_constructor_canonico_conserva_cada_campo() {
        CreateSurgeryCommand command = SurgeryMother.comandoCrear();

        assertThat(command.date()).isEqualTo(SurgeryMother.FECHA);
        assertThat(command.surgeryTypeId()).isEqualTo(SurgeryMother.SURGERY_TYPE_ID);
        assertThat(command.description()).isEqualTo("Ovariohisterectomia electiva");
        assertThat(command.medicament()).isEqualTo("Ketamina 10mg");
        assertThat(command.observations()).isEqualTo("Recuperacion normal");
        assertThat(command.complications()).isNull();
        assertThat(command.animalId()).isEqualTo(SurgeryMother.ANIMAL_ID);
        assertThat(command.consultationId()).isEqualTo(SurgeryMother.CONSULTATION_ID);
        assertThat(command.companyId()).isEqualTo(SurgeryMother.COMPANY_ID);
    }

    @Test
    @DisplayName("la consulta asociada es opcional: viaja en null cuando la cirugia no viene de una")
    void la_consulta_asociada_es_opcional() {
        CreateSurgeryCommand command = SurgeryMother.comandoCrearSinConsulta();

        assertThat(command.consultationId()).isNull();
    }
}
