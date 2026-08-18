package com.vetsoftware.app.companytaxprofile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Algoritmo modulo 11 de la DIAN. Un DV mal calculado no lo rechaza la base de
 * datos: lo rechaza la DIAN cuando ya es tarde para corregir la factura
 * electronica, asi que aqui es donde vale la pena fijar el algoritmo caso a
 * caso y no solo "no lanza excepcion".
 */
@DisplayName("NitVerificationDigit.calculate")
class NitVerificationDigitTest {

    @Nested
    @DisplayName("Validaciones de entrada")
    class Validaciones {

        @ParameterizedTest(name = "nit=[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("rechaza un NIT nulo o en blanco")
        void rechaza_un_nit_nulo_o_en_blanco(String nit) {
            assertThatThrownBy(() -> NitVerificationDigit.calculate(nit))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nit is required");
        }

        @ParameterizedTest(name = "nit=[{0}]")
        @ValueSource(strings = {"90012345A", "900-123456", "900.123456", "900 123456",
                "900123456-8"})
        @DisplayName("rechaza un NIT con caracteres que no son digitos")
        void rechaza_un_nit_con_caracteres_no_numericos(String nit) {
            assertThatThrownBy(() -> NitVerificationDigit.calculate(nit))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only digits");
        }

        @Test
        @DisplayName("rechaza un NIT de mas de quince digitos: no hay peso para el siguiente")
        void rechaza_un_nit_mas_largo_que_la_tabla_de_pesos() {
            assertThatThrownBy(() -> NitVerificationDigit.calculate("1234567890123456"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("too long");
        }

        @Test
        @DisplayName("acepta exactamente quince digitos, el largo maximo de la tabla de pesos")
        void acepta_exactamente_quince_digitos() {
            assertThatCode(() -> NitVerificationDigit.calculate("123456789012345"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("recorta los espacios alrededor antes de validar el formato")
        void recorta_los_espacios_alrededor() {
            assertThat(NitVerificationDigit.calculate("  900123456  ")).isEqualTo("8");
        }
    }

    @Nested
    @DisplayName("Calculo del digito de verificacion")
    class CalculoDelDigito {

        @ParameterizedTest(name = "nit={0} -> dv={1}")
        @CsvSource({
                // NIT real de la Mother: 900123456 -> DV 8 (comentado en
                // CompanyTaxProfileMother).
                "900123456, 8",
                // NIT real usado en el comando de actualizacion de la Mother.
                "830053800, 4",
                // NIT de un solo digito: el peso mas a la derecha de la tabla (3).
                "9, 6",
                // mod == 0: el DV es el propio resto, no 11 - resto.
                "0, 0",
                // mod == 1: mismo caso borde que mod == 0, el otro extremo sin restar de 11.
                "4, 1"})
        @DisplayName("aplica el modulo 11 con los pesos de derecha a izquierda")
        void aplica_el_modulo_11(String nit, String dvEsperado) {
            assertThat(NitVerificationDigit.calculate(nit)).isEqualTo(dvEsperado);
        }

        @Test
        @DisplayName("el DV siempre es un unico caracter, incluso cuando el resto ya es un digito")
        void el_dv_es_siempre_un_unico_caracter() {
            assertThat(NitVerificationDigit.calculate("900123456")).hasSize(1);
        }

        @Test
        @DisplayName("dos NIT distintos que solo cambian un digito dan DV distinto")
        void nits_distintos_dan_dv_distinto() {
            // Documenta que el algoritmo pondera cada posicion: no es una suma de digitos
            // insensible al orden.
            assertThat(NitVerificationDigit.calculate("900123456"))
                    .isNotEqualTo(NitVerificationDigit.calculate("900123457"));
        }

        @Test
        @DisplayName("un cero a la izquierda cuenta como una posicion mas, aunque no cambie el DV")
        void un_cero_a_la_izquierda_cuenta_como_una_posicion_mas() {
            // El cero aporta 0 a la suma sin importar el peso que le toque, asi que el DV
            // no cambia — pero la longitud si se usa para elegir la tabla de pesos: un NIT
            // de dieciseis "0" seguidos de 900123456 seguiria siendo rechazado por largo.
            assertThat(NitVerificationDigit.calculate("0900123456"))
                    .isEqualTo(NitVerificationDigit.calculate("900123456"));
        }
    }
}
