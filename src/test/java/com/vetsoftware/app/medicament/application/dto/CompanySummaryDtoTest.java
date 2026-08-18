package com.vetsoftware.app.medicament.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.medicament.domain.CompanyRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("from() copia id, name e identifier desde el CompanyRef")
    void from_copia_los_campos_del_ref() {
        CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "900123456");

        CompanySummaryDto dto = CompanySummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(9L);
        assertThat(dto.name()).isEqualTo("Clinica Norte");
        assertThat(dto.identifier()).isEqualTo("900123456");
    }
}
