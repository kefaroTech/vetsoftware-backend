package com.vetsoftware.app.company.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.company.domain.CityRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CitySummaryDto.from")
class CitySummaryDtoTest {

    @Test
    @DisplayName("traslada id y nombre del VO sin cruzarlos")
    void traslada_id_y_nombre_del_vo() {
        CitySummaryDto dto = CitySummaryDto.from(new CityRef(11L, "Bogota"));

        assertThat(dto.id()).isEqualTo(11L);
        assertThat(dto.name()).isEqualTo("Bogota");
    }

    @Test
    @DisplayName("el DTO resultante equivale al construido a mano con los mismos valores")
    void equivale_al_construido_a_mano() {
        assertThat(CitySummaryDto.from(new CityRef(12L, "Medellin")))
                .isEqualTo(new CitySummaryDto(12L, "Medellin"));
    }
}
