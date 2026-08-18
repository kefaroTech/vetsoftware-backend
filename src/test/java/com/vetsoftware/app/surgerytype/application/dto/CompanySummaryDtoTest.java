package com.vetsoftware.app.surgerytype.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.surgerytype.domain.CompanyRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("from copia id, nombre e identificador del CompanyRef")
    void from_copia_id_nombre_e_identificador() {
        CompanySummaryDto dto = CompanySummaryDto
                .from(new CompanyRef(9L, "Clinica Norte", "900123456"));

        assertThat(dto.id()).isEqualTo(9L);
        assertThat(dto.name()).isEqualTo("Clinica Norte");
        assertThat(dto.identifier()).isEqualTo("900123456");
    }
}
