package com.vetsoftware.app.vaccination.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.vaccination.domain.CompanyRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("from() copia id, nombre e identificador del ref")
    void from_copia_cada_campo_del_ref() {
        CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900");

        CompanySummaryDto dto = CompanySummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(9L);
        assertThat(dto.name()).isEqualTo("Clinica Norte");
        assertThat(dto.identifier()).isEqualTo("NIT-900");
    }
}
