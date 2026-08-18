package com.vetsoftware.app.companytaxprofile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Companion VO de la actividad economica CIIU. Es opcional en el perfil
 * (persona natural sin actividad declarada) pero, cuando viene, tiene que traer
 * codigo y nombre completos: son los que van a la factura electronica.
 */
@DisplayName("EconomicActivityRef")
class EconomicActivityRefTest {

    @Test
    @DisplayName("acepta id, codigo y nombre y los expone tal cual")
    void acepta_id_codigo_y_nombre() {
        EconomicActivityRef ref = new EconomicActivityRef(5L, "7500", "Actividades veterinarias");

        assertThat(ref.id()).isEqualTo(5L);
        assertThat(ref.code()).isEqualTo("7500");
        assertThat(ref.name()).isEqualTo("Actividades veterinarias");
    }

    @Test
    @DisplayName("rechaza id null")
    void rechaza_id_null() {
        assertThatThrownBy(() -> new EconomicActivityRef(null, "7500", "Actividades veterinarias"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("economic activity id is required");
    }

    @ParameterizedTest(name = "code=[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("rechaza codigo vacio o en blanco")
    void rechaza_codigo_vacio(String codigo) {
        assertThatThrownBy(() -> new EconomicActivityRef(5L, codigo, "Actividades veterinarias"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("economic activity code is required");
    }

    @ParameterizedTest(name = "name=[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("rechaza nombre vacio o en blanco")
    void rechaza_nombre_vacio(String nombre) {
        assertThatThrownBy(() -> new EconomicActivityRef(5L, "7500", nombre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("economic activity name is required");
    }
}
