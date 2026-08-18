package com.vetsoftware.app.vaccinationtype.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.vaccinationtype.testsupport.VaccinationTypeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto.from — mapeo campo por campo")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("copia cada campo del CompanyRef")
    void copia_cada_campo_del_company_ref() {
        CompanySummaryDto dto = CompanySummaryDto.from(VaccinationTypeMother.CLINICA);

        assertThat(dto.id()).isEqualTo(VaccinationTypeMother.CLINICA.id());
        assertThat(dto.name()).isEqualTo(VaccinationTypeMother.CLINICA.name());
        assertThat(dto.identifier()).isEqualTo(VaccinationTypeMother.CLINICA.identifier());
    }
}
