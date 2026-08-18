package com.vetsoftware.app.daycare.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.daycare.testsupport.DayCareMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalSummaryDto.from")
class AnimalSummaryDtoTest {

    @Test
    @DisplayName("mapea cada campo del companion VO, campo por campo")
    void mapea_cada_campo() {
        AnimalSummaryDto dto = AnimalSummaryDto.from(DayCareMother.FIRULAIS);

        assertThat(dto.id()).isEqualTo(DayCareMother.FIRULAIS.id());
        assertThat(dto.name()).isEqualTo(DayCareMother.FIRULAIS.name());
        assertThat(dto.code()).isEqualTo(DayCareMother.FIRULAIS.code());
    }
}
