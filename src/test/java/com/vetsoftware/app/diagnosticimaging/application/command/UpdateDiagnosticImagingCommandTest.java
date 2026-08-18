package com.vetsoftware.app.diagnosticimaging.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateDiagnosticImagingCommand")
class UpdateDiagnosticImagingCommandTest {

    @Test
    @DisplayName("conserva cada campo en su sitio, incluido el id")
    void conserva_cada_campo_en_su_sitio() {
        UpdateDiagnosticImagingCommand comando = DiagnosticImagingMother.comandoActualizar();

        assertThat(comando.id()).isEqualTo(DiagnosticImagingMother.IMAGING_ID);
        assertThat(comando.date()).isEqualTo(DiagnosticImagingMother.FECHA);
        assertThat(comando.diagnosticImagingTypeId()).isEqualTo(DiagnosticImagingMother.TYPE_ID);
        assertThat(comando.clinicalSigns()).isEqualTo("Cojera pata trasera");
        assertThat(comando.studyType()).isEqualTo("Radiografia de cadera actualizada");
        assertThat(comando.diagnosis()).isEqualTo("Displasia moderada");
        assertThat(comando.observations()).isEqualTo("Control en 15 dias");
        assertThat(comando.animalId()).isEqualTo(DiagnosticImagingMother.ANIMAL_ID);
        assertThat(comando.consultationId()).isEqualTo(DiagnosticImagingMother.CONSULTATION_ID);
        assertThat(comando.companyId()).isEqualTo(DiagnosticImagingMother.COMPANY_ID);
    }
}
