package com.vetsoftware.app.laboratorytesttype.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.laboratorytesttype.domain.CompanyRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto.from")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("copia id, nombre e identificador del CompanyRef")
    void copia_los_tres_campos_del_company_ref() {
        CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900");

        CompanySummaryDto dto = CompanySummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(9L);
        assertThat(dto.name()).isEqualTo("Clinica Norte");
        assertThat(dto.identifier()).isEqualTo("NIT-900");
    }
}
