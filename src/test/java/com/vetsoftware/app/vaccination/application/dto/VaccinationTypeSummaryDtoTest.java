package com.vetsoftware.app.vaccination.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.vaccination.domain.VaccinationTypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VaccinationTypeSummaryDto")
class VaccinationTypeSummaryDtoTest {

    @Test
    @DisplayName("from() copia id y nombre del ref")
    void from_copia_cada_campo_del_ref() {
        VaccinationTypeRef ref = new VaccinationTypeRef(1L, "Rabia");

        VaccinationTypeSummaryDto dto = VaccinationTypeSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Rabia");
    }
}
