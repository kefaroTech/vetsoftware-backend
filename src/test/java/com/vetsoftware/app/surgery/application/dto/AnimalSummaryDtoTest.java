package com.vetsoftware.app.surgery.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.surgery.domain.AnimalRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalSummaryDto.from")
class AnimalSummaryDtoTest {

    @Test
    @DisplayName("copia cada campo del companion VO")
    void copia_cada_campo_del_companion_vo() {
        AnimalRef ref = new AnimalRef(100L, "Firulais", "A-001");

        AnimalSummaryDto dto = AnimalSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.name()).isEqualTo("Firulais");
        assertThat(dto.code()).isEqualTo("A-001");
    }
}
