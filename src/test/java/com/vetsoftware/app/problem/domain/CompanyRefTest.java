package com.vetsoftware.app.problem.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyRef — invariantes del value object")
class CompanyRefTest {

    @Nested
    @DisplayName("construccion valida")
    class ConstruccionValida {

        @Test
        @DisplayName("conserva cada campo en su posicion")
        void conserva_cada_campo_en_su_posicion() {
            CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900123");

            assertThat(ref.id()).isEqualTo(9L);
            assertThat(ref.name()).isEqualTo("Clinica Norte");
            assertThat(ref.identifier()).isEqualTo("NIT-900123");
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        @Test
        @DisplayName("id nulo se rechaza")
        void id_nulo_se_rechaza() {
            assertThatThrownBy(() -> new CompanyRef(null, "Clinica Norte", "NIT-900123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @Test
        @DisplayName("nombre nulo se rechaza")
        void nombre_nulo_se_rechaza() {
            assertThatThrownBy(() -> new CompanyRef(9L, null, "NIT-900123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @Test
        @DisplayName("nombre en blanco se rechaza")
        void nombre_en_blanco_se_rechaza() {
            assertThatThrownBy(() -> new CompanyRef(9L, "   ", "NIT-900123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @Test
        @DisplayName("identificador nulo se rechaza")
        void identificador_nulo_se_rechaza() {
            assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Norte", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }

        @Test
        @DisplayName("identificador en blanco se rechaza")
        void identificador_en_blanco_se_rechaza() {
            assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Norte", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }
    }
}
