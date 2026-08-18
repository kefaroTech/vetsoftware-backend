package com.vetsoftware.app.diagnosticimaging.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalSummaryDto")
class AnimalSummaryDtoTest {

    @Test
    @DisplayName("from(AnimalRef) copia cada campo")
    void from_copia_cada_campo() {
        AnimalSummaryDto dto = AnimalSummaryDto.from(DiagnosticImagingMother.MASCOTA);

        assertThat(dto.id()).isEqualTo(DiagnosticImagingMother.MASCOTA.id());
        assertThat(dto.name()).isEqualTo(DiagnosticImagingMother.MASCOTA.name());
        assertThat(dto.code()).isEqualTo(DiagnosticImagingMother.MASCOTA.code());
    }
}
