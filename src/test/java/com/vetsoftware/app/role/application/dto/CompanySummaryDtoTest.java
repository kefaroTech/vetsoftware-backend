package com.vetsoftware.app.role.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.role.domain.CompanyRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto — from(CompanyRef)")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("copia cada campo del companion VO")
    void copia_cada_campo_del_companion_vo() {
        CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900");

        CompanySummaryDto dto = CompanySummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(9L);
        assertThat(dto.name()).isEqualTo("Clinica Norte");
        assertThat(dto.identifier()).isEqualTo("NIT-900");
    }
}
