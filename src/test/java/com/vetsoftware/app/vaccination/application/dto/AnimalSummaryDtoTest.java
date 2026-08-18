package com.vetsoftware.app.vaccination.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.vaccination.domain.AnimalRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalSummaryDto")
class AnimalSummaryDtoTest {

    @Test
    @DisplayName("from() copia id, nombre y codigo del ref")
    void from_copia_cada_campo_del_ref() {
        AnimalRef ref = new AnimalRef(3L, "Firulais", "A-001");

        AnimalSummaryDto dto = AnimalSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(3L);
        assertThat(dto.name()).isEqualTo("Firulais");
        assertThat(dto.code()).isEqualTo("A-001");
    }
}
