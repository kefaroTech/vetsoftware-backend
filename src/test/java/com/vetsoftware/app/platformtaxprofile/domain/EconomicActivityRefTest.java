package com.vetsoftware.app.platformtaxprofile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("EconomicActivityRef")
class EconomicActivityRefTest {

    @Test
    @DisplayName("se construye con los tres campos")
    void se_construye_con_los_tres_campos() {
        EconomicActivityRef ref = new EconomicActivityRef(11L, "6201", "Desarrollo de software");

        assertThat(ref.id()).isEqualTo(11L);
        assertThat(ref.code()).isEqualTo("6201");
        assertThat(ref.name()).isEqualTo("Desarrollo de software");
    }

    @Test
    @DisplayName("el id es obligatorio")
    void el_id_es_obligatorio() {
        assertThatThrownBy(() -> new EconomicActivityRef(null, "6201", "Desarrollo de software"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("economic activity id is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("el codigo no puede estar en blanco")
    void el_codigo_no_puede_estar_en_blanco(String codigo) {
        assertThatThrownBy(() -> new EconomicActivityRef(11L, codigo, "Desarrollo de software"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("economic activity code is required");
    }

    @Test
    @DisplayName("el codigo nulo se rechaza")
    void el_codigo_nulo_se_rechaza() {
        assertThatThrownBy(() -> new EconomicActivityRef(11L, null, "Desarrollo de software"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("economic activity code is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("el nombre no puede estar en blanco")
    void el_nombre_no_puede_estar_en_blanco(String nombre) {
        assertThatThrownBy(() -> new EconomicActivityRef(11L, "6201", nombre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("economic activity name is required");
    }

    @Test
    @DisplayName("el nombre nulo se rechaza")
    void el_nombre_nulo_se_rechaza() {
        assertThatThrownBy(() -> new EconomicActivityRef(11L, "6201", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("economic activity name is required");
    }
}
