package com.vetsoftware.app.spa.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.spa.testsupport.SpaMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalSummaryDto.from")
class AnimalSummaryDtoTest {

    @Test
    @DisplayName("mapea cada campo del companion VO, campo por campo")
    void mapea_cada_campo() {
        AnimalSummaryDto dto = AnimalSummaryDto.from(SpaMother.FIRULAIS);

        assertThat(dto.id()).isEqualTo(SpaMother.FIRULAIS.id());
        assertThat(dto.name()).isEqualTo(SpaMother.FIRULAIS.name());
        assertThat(dto.code()).isEqualTo(SpaMother.FIRULAIS.code());
    }
}
