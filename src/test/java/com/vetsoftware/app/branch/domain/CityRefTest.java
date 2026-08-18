package com.vetsoftware.app.branch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Companion VO de {@code branch} hacia {@code city} (cross-feature reference).
 * VO propio de esta feature, homónimo del {@code CityRef} de otras features.
 */
@DisplayName("CityRef")
class CityRefTest {

    @Nested
    @DisplayName("construcción válida")
    class Creacion {

        @Test
        @DisplayName("expone id y nombre tal cual se construyó")
        void expone_los_campos_tal_cual_se_construyo() {
            CityRef ref = new CityRef(5L, "Bogotá");

            assertThat(ref.id()).isEqualTo(5L);
            assertThat(ref.name()).isEqualTo("Bogotá");
        }
    }

    @Nested
    @DisplayName("invariantes")
    class Validaciones {

        @Test
        @DisplayName("el id es obligatorio")
        void el_id_es_obligatorio() {
            assertThatThrownBy(() -> new CityRef(null, "Bogotá"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("city id is required");
        }

        @ParameterizedTest(name = "nombre inválido: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("el nombre es obligatorio: nulo, vacío o en blanco")
        void el_nombre_es_obligatorio(String nombreInvalido) {
            assertThatThrownBy(() -> new CityRef(5L, nombreInvalido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("city name is required");
        }
    }
}
