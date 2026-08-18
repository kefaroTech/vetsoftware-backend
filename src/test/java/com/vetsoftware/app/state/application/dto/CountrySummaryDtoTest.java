package com.vetsoftware.app.state.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.state.domain.CountryRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CountrySummaryDto — proyeccion del companion VO")
class CountrySummaryDtoTest {

    @Test
    @DisplayName("from copia id y nombre del ref")
    void from_copia_id_y_nombre() {
        CountrySummaryDto dto = CountrySummaryDto.from(new CountryRef(7L, "Colombia"));

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Colombia");
    }
}
