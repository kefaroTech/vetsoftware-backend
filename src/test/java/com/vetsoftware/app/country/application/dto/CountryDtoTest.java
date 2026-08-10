package com.vetsoftware.app.country.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.country.domain.Country;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CountryDto — proyeccion del agregado")
class CountryDtoTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Test
    @DisplayName("from copia los cuatro campos del pais")
    void from_copia_los_cuatro_campos() {
        CountryDto dto = CountryDto.from(new Country(7L, "Colombia", CREACION, true));

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Colombia");
        assertThat(dto.createdDate()).isEqualTo(CREACION);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from propaga el id nulo de un pais recien creado")
    void from_propaga_el_id_nulo() {
        CountryDto dto = CountryDto.from(new Country(null, "Colombia", CREACION, true));

        assertThat(dto.id()).isNull();
    }

    @Test
    @DisplayName("from refleja el estado deshabilitado")
    void from_refleja_el_estado_deshabilitado() {
        CountryDto dto = CountryDto.from(new Country(7L, "Colombia", CREACION, false));

        assertThat(dto.enabled()).isFalse();
    }
}
