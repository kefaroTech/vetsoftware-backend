package com.vetsoftware.app.supplier.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.supplier.domain.CompanyRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto.from — mapeo campo por campo")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("mapea id, name e identifier desde el CompanyRef")
    void mapea_id_name_e_identifier() {
        CompanyRef ref = new CompanyRef(10L, "Clinica Norte", "900123456");

        CompanySummaryDto dto = CompanySummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.name()).isEqualTo("Clinica Norte");
        assertThat(dto.identifier()).isEqualTo("900123456");
    }
}
