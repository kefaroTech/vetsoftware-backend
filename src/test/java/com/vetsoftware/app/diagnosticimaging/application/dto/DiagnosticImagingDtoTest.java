package com.vetsoftware.app.diagnosticimaging.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DiagnosticImagingDto")
class DiagnosticImagingDtoTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("copia cada campo de la imagen diagnostica, incluida la consulta")
        void copia_cada_campo_incluida_la_consulta() {
            DiagnosticImaging imaging = DiagnosticImagingMother.persistida();

            DiagnosticImagingDto dto = DiagnosticImagingDto.from(imaging);

            assertThat(dto.id()).isEqualTo(imaging.getId());
            assertThat(dto.date()).isEqualTo(imaging.getDate());
            assertThat(dto.diagnosticImagingType().id())
                    .isEqualTo(imaging.getDiagnosticImagingType().id());
            assertThat(dto.clinicalSigns()).isEqualTo(imaging.getClinicalSigns());
            assertThat(dto.studyType()).isEqualTo(imaging.getStudyType());
            assertThat(dto.diagnosis()).isEqualTo(imaging.getDiagnosis());
            assertThat(dto.observations()).isEqualTo(imaging.getObservations());
            assertThat(dto.status()).isEqualTo(imaging.getStatus().name());
            assertThat(dto.animal().id()).isEqualTo(imaging.getAnimal().id());
            assertThat(dto.consultation().id()).isEqualTo(imaging.getConsultation().id());
            assertThat(dto.company().id()).isEqualTo(imaging.getCompany().id());
            assertThat(dto.createdDate()).isEqualTo(imaging.getCreatedDate());
            assertThat(dto.enabled()).isEqualTo(imaging.isEnabled());
        }

        @Test
        @DisplayName("una imagen sin consulta asociada deja consultation en null")
        void sin_consulta_asociada_deja_consultation_en_null() {
            DiagnosticImaging imaging = DiagnosticImagingMother.sinConsulta();

            DiagnosticImagingDto dto = DiagnosticImagingDto.from(imaging);

            assertThat(dto.consultation()).isNull();
        }
    }
}
