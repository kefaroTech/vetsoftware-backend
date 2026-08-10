package com.vetsoftware.app.company.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CityRef — VO de la ciudad referenciada")
class CityRefTest {

    @Test
    @DisplayName("conserva id y nombre en su posicion")
    void conserva_id_y_nombre() {
        CityRef ref = new CityRef(11L, "Bogota");

        assertThat(ref.id()).isEqualTo(11L);
        assertThat(ref.name()).isEqualTo("Bogota");
    }

    @Test
    @DisplayName("rechaza el id nulo: sin id no hay referencia que resolver")
    void rechaza_el_id_nulo() {
        assertThatThrownBy(() -> new CityRef(null, "Bogota"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("city id is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("rechaza el nombre ausente o en blanco")
    void rechaza_el_nombre_en_blanco(String nombre) {
        assertThatThrownBy(() -> new CityRef(11L, nombre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("city name is required");
    }

    @Test
    @DisplayName("dos referencias con los mismos valores son iguales")
    void dos_referencias_con_los_mismos_valores_son_iguales() {
        assertThat(new CityRef(11L, "Bogota")).isEqualTo(new CityRef(11L, "Bogota"))
                .isNotEqualTo(new CityRef(12L, "Bogota"));
    }
}
