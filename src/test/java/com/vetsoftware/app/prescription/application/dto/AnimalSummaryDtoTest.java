package com.vetsoftware.app.prescription.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalSummaryDto")
class AnimalSummaryDtoTest {

    @Test
    @DisplayName("from(AnimalRef) copia cada campo")
    void from_copia_cada_campo() {
        AnimalSummaryDto dto = AnimalSummaryDto.from(PrescriptionMother.MASCOTA);

        assertThat(dto.id()).isEqualTo(PrescriptionMother.MASCOTA.id());
        assertThat(dto.name()).isEqualTo(PrescriptionMother.MASCOTA.name());
        assertThat(dto.code()).isEqualTo(PrescriptionMother.MASCOTA.code());
    }
}
