package com.vetsoftware.app.diagnosticimaging.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DiagnosticImagingTypeSummaryDto")
class DiagnosticImagingTypeSummaryDtoTest {

    @Test
    @DisplayName("from(DiagnosticImagingTypeRef) copia cada campo")
    void from_copia_cada_campo() {
        DiagnosticImagingTypeSummaryDto dto = DiagnosticImagingTypeSummaryDto
                .from(DiagnosticImagingMother.TIPO);

        assertThat(dto.id()).isEqualTo(DiagnosticImagingMother.TIPO.id());
        assertThat(dto.name()).isEqualTo(DiagnosticImagingMother.TIPO.name());
    }
}
