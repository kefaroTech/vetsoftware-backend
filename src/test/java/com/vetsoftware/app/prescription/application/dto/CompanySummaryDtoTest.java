package com.vetsoftware.app.prescription.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("from(CompanyRef) copia cada campo")
    void from_copia_cada_campo() {
        CompanySummaryDto dto = CompanySummaryDto.from(PrescriptionMother.CLINICA);

        assertThat(dto.id()).isEqualTo(PrescriptionMother.CLINICA.id());
        assertThat(dto.name()).isEqualTo(PrescriptionMother.CLINICA.name());
        assertThat(dto.identifier()).isEqualTo(PrescriptionMother.CLINICA.identifier());
    }
}
