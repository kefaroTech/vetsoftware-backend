package com.vetsoftware.app.supplierinvoice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests del VO {@link CompanyRef}: identidad de la empresa dueña de la factura.
 */
class CompanyRefTest {

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("conserva id, nombre e identificador tal cual")
        void conserva_id_nombre_e_identificador() {
            CompanyRef ref = new CompanyRef(1L, "Clinica Norte", "NIT-900");

            assertThat(ref.id()).isEqualTo(1L);
            assertThat(ref.name()).isEqualTo("Clinica Norte");
            assertThat(ref.identifier()).isEqualTo("NIT-900");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("id nulo es invalido")
        void id_nulo_es_invalido() {
            assertThatThrownBy(() -> new CompanyRef(null, "Clinica Norte", "NIT-900"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @ParameterizedTest
        @DisplayName("nombre nulo o en blanco es invalido")
        @ValueSource(strings = {"", "   "})
        void nombre_en_blanco_es_invalido(String nombreInvalido) {
            assertThatThrownBy(() -> new CompanyRef(1L, nombreInvalido, "NIT-900"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @Test
        @DisplayName("nombre nulo es invalido")
        void nombre_nulo_es_invalido() {
            assertThatThrownBy(() -> new CompanyRef(1L, null, "NIT-900"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @ParameterizedTest
        @DisplayName("identificador nulo o en blanco es invalido")
        @ValueSource(strings = {"", "   "})
        void identificador_en_blanco_es_invalido(String identificadorInvalido) {
            assertThatThrownBy(() -> new CompanyRef(1L, "Clinica Norte", identificadorInvalido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }

        @Test
        @DisplayName("identificador nulo es invalido")
        void identificador_nulo_es_invalido() {
            assertThatThrownBy(() -> new CompanyRef(1L, "Clinica Norte", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }
    }
}
