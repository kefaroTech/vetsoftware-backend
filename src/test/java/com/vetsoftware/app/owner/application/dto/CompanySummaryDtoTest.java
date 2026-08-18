package com.vetsoftware.app.owner.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.owner.domain.CompanyRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto.from")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("copia id, nombre e identificador desde el companion VO")
    void copia_id_nombre_e_identificador_desde_el_companion_vo() {
        CompanySummaryDto dto = CompanySummaryDto
                .from(new CompanyRef(9L, "Clinica Norte", "NIT-900123456"));

        assertThat(dto.id()).isEqualTo(9L);
        assertThat(dto.name()).isEqualTo("Clinica Norte");
        assertThat(dto.identifier()).isEqualTo("NIT-900123456");
    }
}
