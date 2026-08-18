package com.vetsoftware.app.daycare.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.daycare.testsupport.DayCareMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto.from")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("mapea cada campo del companion VO, campo por campo")
    void mapea_cada_campo() {
        CompanySummaryDto dto = CompanySummaryDto.from(DayCareMother.CLINICA);

        assertThat(dto.id()).isEqualTo(DayCareMother.CLINICA.id());
        assertThat(dto.name()).isEqualTo(DayCareMother.CLINICA.name());
        assertThat(dto.identifier()).isEqualTo(DayCareMother.CLINICA.identifier());
    }
}
