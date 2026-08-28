package com.vetsoftware.app.accountingperiod.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AccountingPeriodKey — la clave de mes yyyy-MM")
class AccountingPeriodKeyTest {

    @Nested
    @DisplayName("Formato")
    class Formato {

        @Test
        @DisplayName("acepta un mes bien escrito y lo conserva tal cual")
        void acepta_un_mes_bien_escrito() {
            assertThat(AccountingPeriodKey.of("2026-03").value()).isEqualTo("2026-03");
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-3", "2026-13", "2026-00", "26-03", "2026/03", "2026-03-01",
                "202A-03", " 2026-03"})
        @DisplayName("rechaza todo lo que el CHECK del esquema rechazaria")
        void rechaza_lo_que_el_check_rechazaria(String candidata) {
            // Espejo de chk_accounting_periods_key. Sin esta comprobacion aqui, cada
            // uno de estos valores llegaria al motor y volveria como un error de
            // comprobacion que no nombra ni la columna ni el valor.
            assertThatThrownBy(() -> AccountingPeriodKey.of(candidata))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("periodKey must have the form yyyy-MM");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("una clave vacia o en blanco se rechaza nombrando el campo")
        void una_clave_vacia_se_rechaza(String vacia) {
            assertThatThrownBy(() -> AccountingPeriodKey.of(vacia))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("periodKey is required");
        }

        @Test
        @DisplayName("una clave nula se rechaza nombrando el campo")
        void una_clave_nula_se_rechaza() {
            assertThatThrownBy(() -> AccountingPeriodKey.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("periodKey is required");
        }
    }

    @Nested
    @DisplayName("Derivacion desde una fecha")
    class Derivacion {

        @Test
        @DisplayName("un mes de un solo digito sale con cero a la izquierda")
        void un_mes_de_un_digito_sale_con_cero() {
            // Sin el cero, "2026-9" ordenaria DESPUES de "2026-10" y la resolucion del
            // periodo de imputacion devolveria el mes equivocado sin fallar.
            assertThat(AccountingPeriodKey.from(LocalDate.of(2026, 9, 30)).value())
                    .isEqualTo("2026-09");
        }

        @Test
        @DisplayName("el 31 de diciembre pertenece a diciembre, no al ano siguiente")
        void el_31_de_diciembre_pertenece_a_diciembre() {
            // El caso que caza el uso de YYYY (ano de la semana ISO) en vez de yyyy: con
            // el patron equivocado esta fecha devuelve 2027-12 y desvia el asiento de
            // ejercicio.
            assertThat(AccountingPeriodKey.from(LocalDate.of(2026, 12, 31)).value())
                    .isEqualTo("2026-12");
        }

        @Test
        @DisplayName("una fecha nula se rechaza nombrando el campo")
        void una_fecha_nula_se_rechaza() {
            assertThatThrownBy(() -> AccountingPeriodKey.from(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("date is required");
        }
    }

    @Nested
    @DisplayName("Orden")
    class Orden {

        @Test
        @DisplayName("el orden lexicografico coincide con el cronologico en el cambio de decena")
        void el_orden_lexicografico_coincide_con_el_cronologico() {
            // Septiembre a octubre es el unico salto donde un formato descuidado se
            // rompe, y es de lo que depende findFirstOpenFrom.
            assertThat(
                    AccountingPeriodKey.of("2026-09").isBefore(AccountingPeriodKey.of("2026-10")))
                    .isTrue();
            assertThat(AccountingPeriodKey.of("2026-10").isAfter(AccountingPeriodKey.of("2026-09")))
                    .isTrue();
        }

        @Test
        @DisplayName("el cambio de ano ordena por el ano, no por el mes")
        void el_cambio_de_ano_ordena_por_el_ano() {
            assertThat(
                    AccountingPeriodKey.of("2026-12").isBefore(AccountingPeriodKey.of("2027-01")))
                    .isTrue();
        }

        @Test
        @DisplayName("dos claves iguales no son ni anterior ni posterior")
        void dos_claves_iguales_no_son_ni_anterior_ni_posterior() {
            AccountingPeriodKey marzo = AccountingPeriodKey.of("2026-03");

            assertThat(marzo.compareTo(AccountingPeriodKey.of("2026-03"))).isZero();
            assertThat(marzo.isAfter(AccountingPeriodKey.of("2026-03"))).isFalse();
            assertThat(marzo.isBefore(AccountingPeriodKey.of("2026-03"))).isFalse();
        }

        @Test
        @DisplayName("se imprime como la cadena que es, sin envoltorio")
        void se_imprime_como_la_cadena_que_es() {
            // Va en los mensajes de excepcion: un "AccountingPeriodKey[value=2026-03]"
            // en un 409 obliga a quien lo lee a traducirlo.
            assertThat(AccountingPeriodKey.of("2026-03")).hasToString("2026-03");
        }
    }
}
