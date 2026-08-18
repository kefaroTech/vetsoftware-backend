package com.vetsoftware.app.surgerytype.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyRef (surgerytype)")
class CompanyRefTest {

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("conserva id, nombre e identificador")
        void conserva_id_nombre_e_identificador() {
            CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "900123456");

            assertThat(ref.id()).isEqualTo(9L);
            assertThat(ref.name()).isEqualTo("Clinica Norte");
            assertThat(ref.identifier()).isEqualTo("900123456");
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("el id nulo no es valido")
        void el_id_nulo_no_es_valido() {
            assertThatThrownBy(() -> new CompanyRef(null, "Clinica Norte", "900123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @Test
        @DisplayName("el nombre nulo no es valido")
        void el_nombre_nulo_no_es_valido() {
            assertThatThrownBy(() -> new CompanyRef(9L, null, "900123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @Test
        @DisplayName("el nombre en blanco no es valido")
        void el_nombre_en_blanco_no_es_valido() {
            assertThatThrownBy(() -> new CompanyRef(9L, "   ", "900123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @Test
        @DisplayName("el identificador nulo no es valido")
        void el_identificador_nulo_no_es_valido() {
            assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Norte", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }

        @Test
        @DisplayName("el identificador en blanco no es valido")
        void el_identificador_en_blanco_no_es_valido() {
            assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Norte", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }
    }
}
