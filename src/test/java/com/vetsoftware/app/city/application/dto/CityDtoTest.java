package com.vetsoftware.app.city.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.city.domain.City;
import com.vetsoftware.app.city.testsupport.CityMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CityDto.from — mapeo campo por campo")
class CityDtoTest {

    @Test
    @DisplayName("copia cada campo de la ciudad activa")
    void copia_cada_campo_de_la_ciudad_activa() {
        City ciudad = CityMother.activa();

        CityDto dto = CityDto.from(ciudad);

        assertThat(dto.id()).isEqualTo(ciudad.getId());
        assertThat(dto.name()).isEqualTo(ciudad.getName());
        assertThat(dto.state()).isEqualTo(StateSummaryDto.from(ciudad.getState()));
        assertThat(dto.daneCode()).isEqualTo(ciudad.getDaneCode());
        assertThat(dto.createdDate()).isEqualTo(ciudad.getCreatedDate());
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("una ciudad deshabilitada mapea enabled en falso")
    void una_ciudad_deshabilitada_mapea_enabled_en_falso() {
        City ciudad = CityMother.deshabilitada();

        CityDto dto = CityDto.from(ciudad);

        assertThat(dto.enabled()).isFalse();
    }
}
