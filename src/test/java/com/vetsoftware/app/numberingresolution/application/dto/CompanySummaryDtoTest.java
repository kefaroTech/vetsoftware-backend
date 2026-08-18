package com.vetsoftware.app.numberingresolution.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.numberingresolution.domain.CompanyRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("from() copia id, nombre e identificador de la referencia")
    void from_copia_los_tres_campos() {
        CompanyRef ref = new CompanyRef(9L, "Veterinaria Central", "900123456");

        CompanySummaryDto dto = CompanySummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(9L);
        assertThat(dto.name()).isEqualTo("Veterinaria Central");
        assertThat(dto.identifier()).isEqualTo("900123456");
    }
}
