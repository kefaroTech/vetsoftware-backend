package com.vetsoftware.app.companytaxprofile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Codigo de responsabilidad DIAN (O-13, O-15, O-23, ...). El VO no valida
 * contra el catalogo oficial completo, solo forma: obligatorio y hasta diez
 * caracteres, lo mismo que la columna que lo persiste.
 */
@DisplayName("CompanyTaxProfileResponsibility")
class CompanyTaxProfileResponsibilityTest {

    @Nested
    @DisplayName("Construccion")
    class Construccion {

        @Test
        @DisplayName("conserva el codigo tal cual")
        void conserva_el_codigo_tal_cual() {
            CompanyTaxProfileResponsibility responsibility = new CompanyTaxProfileResponsibility(
                    "O-13");

            assertThat(responsibility.code()).isEqualTo("O-13");
        }

        @Test
        @DisplayName("acepta un codigo de exactamente diez caracteres")
        void acepta_un_codigo_de_diez_caracteres() {
            CompanyTaxProfileResponsibility responsibility = new CompanyTaxProfileResponsibility(
                    "1234567890");

            assertThat(responsibility.code()).hasSize(10);
        }
    }

    @Nested
    @DisplayName("Invariantes")
    class Invariantes {

        @ParameterizedTest(name = "code=[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("rechaza un codigo nulo o en blanco")
        void rechaza_un_codigo_nulo_o_en_blanco(String code) {
            assertThatThrownBy(() -> new CompanyTaxProfileResponsibility(code))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("responsibility code is required");
        }

        @Test
        @DisplayName("rechaza un codigo de mas de diez caracteres")
        void rechaza_un_codigo_de_mas_de_diez_caracteres() {
            assertThatThrownBy(() -> new CompanyTaxProfileResponsibility("12345678901"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("10 chars or less");
        }
    }
}
