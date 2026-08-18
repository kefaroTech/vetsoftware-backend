package com.vetsoftware.app.supplierinvoice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests del VO {@link SupplierRef}: proveedor que emite la factura. */
class SupplierRefTest {

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("conserva id, nombre y NIT tal cual")
        void conserva_id_nombre_y_nit() {
            SupplierRef ref = new SupplierRef(7L, "Distribuidora Sur", "800111222");

            assertThat(ref.id()).isEqualTo(7L);
            assertThat(ref.name()).isEqualTo("Distribuidora Sur");
            assertThat(ref.taxId()).isEqualTo("800111222");
        }

        @Test
        @DisplayName("el NIT es opcional")
        void el_nit_es_opcional() {
            SupplierRef ref = new SupplierRef(7L, "Distribuidora Sur", null);

            assertThat(ref.taxId()).isNull();
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("id nulo es invalido")
        void id_nulo_es_invalido() {
            assertThatThrownBy(() -> new SupplierRef(null, "Distribuidora Sur", "800111222"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("supplier id is required");
        }

        @Test
        @DisplayName("nombre nulo es invalido")
        void nombre_nulo_es_invalido() {
            assertThatThrownBy(() -> new SupplierRef(7L, null, "800111222"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("supplier name is required");
        }

        @ParameterizedTest
        @DisplayName("nombre en blanco es invalido")
        @ValueSource(strings = {"", "   "})
        void nombre_en_blanco_es_invalido(String nombreInvalido) {
            assertThatThrownBy(() -> new SupplierRef(7L, nombreInvalido, "800111222"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("supplier name is required");
        }
    }
}
