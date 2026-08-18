package com.vetsoftware.app.diagnosticimaging.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("from(CompanyRef) copia cada campo")
    void from_copia_cada_campo() {
        CompanySummaryDto dto = CompanySummaryDto.from(DiagnosticImagingMother.EMPRESA);

        assertThat(dto.id()).isEqualTo(DiagnosticImagingMother.EMPRESA.id());
        assertThat(dto.name()).isEqualTo(DiagnosticImagingMother.EMPRESA.name());
        assertThat(dto.identifier()).isEqualTo(DiagnosticImagingMother.EMPRESA.identifier());
    }
}
