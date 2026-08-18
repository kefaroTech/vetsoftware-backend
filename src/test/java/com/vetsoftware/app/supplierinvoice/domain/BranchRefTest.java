package com.vetsoftware.app.supplierinvoice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests del VO {@link BranchRef}: sede a la que se imputa la factura de
 * proveedor.
 */
class BranchRefTest {

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("conserva id y nombre tal cual")
        void conserva_id_y_nombre() {
            BranchRef ref = new BranchRef(3L, "Sede Centro");

            assertThat(ref.id()).isEqualTo(3L);
            assertThat(ref.name()).isEqualTo("Sede Centro");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("id nulo es invalido")
        void id_nulo_es_invalido() {
            assertThatThrownBy(() -> new BranchRef(null, "Sede Centro"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("branch id is required");
        }

        @Test
        @DisplayName("nombre nulo es invalido")
        void nombre_nulo_es_invalido() {
            assertThatThrownBy(() -> new BranchRef(3L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("branch name is required");
        }

        @ParameterizedTest
        @DisplayName("nombre en blanco es invalido")
        @ValueSource(strings = {"", "   "})
        void nombre_en_blanco_es_invalido(String nombreInvalido) {
            assertThatThrownBy(() -> new BranchRef(3L, nombreInvalido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("branch name is required");
        }
    }
}
