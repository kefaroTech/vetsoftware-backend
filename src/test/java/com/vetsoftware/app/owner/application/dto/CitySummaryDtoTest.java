package com.vetsoftware.app.owner.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.owner.domain.CityRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CitySummaryDto.from")
class CitySummaryDtoTest {

    @Test
    @DisplayName("copia id y nombre desde el companion VO")
    void copia_id_y_nombre_desde_el_companion_vo() {
        CitySummaryDto dto = CitySummaryDto.from(new CityRef(5L, "Bogota"));

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.name()).isEqualTo("Bogota");
    }
}
