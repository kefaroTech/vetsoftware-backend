package com.vetsoftware.app.breed.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SpecieRef — invariantes del VO de referencia")
class SpecieRefTest {

    @Test
    @DisplayName("conserva id y nombre validos")
    void conserva_id_y_nombre_validos() {
        SpecieRef ref = new SpecieRef(1L, "Perro");

        assertThat(ref.id()).isEqualTo(1L);
        assertThat(ref.name()).isEqualTo("Perro");
    }

    @Test
    @DisplayName("rechaza id nulo")
    void rechaza_id_nulo() {
        assertThatThrownBy(() -> new SpecieRef(null, "Perro"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("specie id is required");
    }

    @Test
    @DisplayName("rechaza nombre nulo")
    void rechaza_nombre_nulo() {
        assertThatThrownBy(() -> new SpecieRef(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("specie name is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("rechaza nombre vacio o en blanco")
    void rechaza_nombre_vacio_o_en_blanco(String nombre) {
        assertThatThrownBy(() -> new SpecieRef(1L, nombre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("specie name is required");
    }
}
