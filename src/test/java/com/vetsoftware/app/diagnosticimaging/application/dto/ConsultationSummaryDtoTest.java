package com.vetsoftware.app.diagnosticimaging.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationSummaryDto")
class ConsultationSummaryDtoTest {

    @Test
    @DisplayName("from(ConsultationRef) copia cada campo")
    void from_copia_cada_campo() {
        ConsultationSummaryDto dto = ConsultationSummaryDto.from(DiagnosticImagingMother.CONSULTA);

        assertThat(dto.id()).isEqualTo(DiagnosticImagingMother.CONSULTA.id());
        assertThat(dto.date()).isEqualTo(DiagnosticImagingMother.CONSULTA.date());
    }
}
