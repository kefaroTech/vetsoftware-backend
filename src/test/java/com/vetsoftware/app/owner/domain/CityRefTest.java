package com.vetsoftware.app.owner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CityRef — companion VO de city")
class CityRefTest {

    @Test
    @DisplayName("conserva cada campo tal y como se le entrega")
    void conserva_cada_campo_tal_y_como_se_le_entrega() {
        CityRef ref = new CityRef(5L, "Bogota");

        assertThat(ref.id()).isEqualTo(5L);
        assertThat(ref.name()).isEqualTo("Bogota");
    }

    @Test
    @DisplayName("rechaza id nulo")
    void rechaza_id_nulo() {
        assertThatThrownBy(() -> new CityRef(null, "Bogota"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("city id is required");
    }

    @Test
    @DisplayName("rechaza nombre nulo")
    void rechaza_nombre_nulo() {
        assertThatThrownBy(() -> new CityRef(5L, null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("city name is required");
    }

    @Test
    @DisplayName("rechaza nombre en blanco")
    void rechaza_nombre_en_blanco() {
        assertThatThrownBy(() -> new CityRef(5L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("city name is required");
    }
}
