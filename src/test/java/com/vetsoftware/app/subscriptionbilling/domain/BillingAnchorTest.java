package com.vetsoftware.app.subscriptionbilling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El ancla del reloj de cobro. La prueba que importa es la tercera clase: que
 * <b>no se degrada</b> al pasar por un mes corto.
 */
@DisplayName("BillingAnchor — el dia del mes al que queda anclado el cobro")
class BillingAnchorTest {

    @Nested
    @DisplayName("Construccion")
    class Construccion {

        @Test
        @DisplayName("se deriva del dia de la fecha en que el contrato empieza a devengar")
        void se_deriva_del_dia_de_la_fecha() {
            assertThat(BillingAnchor.from(LocalDate.of(2026, 1, 31)).dayOfMonth()).isEqualTo(31);
        }

        @ParameterizedTest(name = "rechaza el dia {0}")
        @ValueSource(ints = {0, -1, 32})
        @DisplayName("rechaza un dia que ningun mes puede tener")
        void rechaza_un_dia_imposible(int dia) {
            assertThatThrownBy(() -> new BillingAnchor(dia))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("billing anchor day must be between 1 and 31");
        }

        @Test
        @DisplayName("rechaza construirse sin fecha")
        void rechaza_sin_fecha() {
            assertThatThrownBy(() -> BillingAnchor.from(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("anchor date is required");
        }
    }

    @Nested
    @DisplayName("Materializacion sobre un mes")
    class SobreUnMes {

        @Test
        @DisplayName("un mes que llega al dia del ancla lo devuelve tal cual")
        void mes_largo_devuelve_el_dia_del_ancla() {
            assertThat(new BillingAnchor(31).onMonth(java.time.YearMonth.of(2026, 3)))
                    .isEqualTo(LocalDate.of(2026, 3, 31));
        }

        @Test
        @DisplayName("un mes corto recorta al ultimo dia, sin mover el ancla")
        void mes_corto_recorta_al_ultimo_dia() {
            BillingAnchor ancla = new BillingAnchor(31);

            assertThat(ancla.onMonth(java.time.YearMonth.of(2026, 2)))
                    .isEqualTo(LocalDate.of(2026, 2, 28));
            // El recorte es la presentacion en ese mes, no una mutacion: el ancla sigue
            // siendo 31 y el mes siguiente lo demuestra.
            assertThat(ancla.dayOfMonth()).isEqualTo(31);
        }

        @Test
        @DisplayName("febrero bisiesto llega al 29")
        void febrero_bisiesto() {
            assertThat(new BillingAnchor(31).onMonth(java.time.YearMonth.of(2028, 2)))
                    .isEqualTo(LocalDate.of(2028, 2, 29));
        }
    }

    @Nested
    @DisplayName("El ancla no se degrada")
    class NoSeDegrada {

        /**
         * <b>La prueba de la regla.</b> Un contrato anclado al 31 factura el 28 de
         * febrero, el 31 de marzo y el 30 de abril — y sigue anclado al 31. La forma
         * ingenua ({@code periodStart.plusMonths(1)}) daria 28 de febrero y luego 28 de
         * marzo, 28 de abril y 28 para siempre: tres dias de servicio al ano que el
         * cliente pierde sin que nadie lo note, porque cada paso individual se ve
         * correcto.
         */
        @Test
        @DisplayName("anclado al 31: 28 de febrero, 31 de marzo, 30 de abril, y sigue en 31")
        void anclado_al_31_vuelve_a_su_dia_despues_de_febrero() {
            BillingAnchor ancla = BillingAnchor.from(LocalDate.of(2026, 1, 31));

            LocalDate primerCobro = ancla.onMonth(java.time.YearMonth.of(2026, 2));
            LocalDate segundoCobro = ancla.onMonth(java.time.YearMonth.of(2026, 3));
            LocalDate tercerCobro = ancla.onMonth(java.time.YearMonth.of(2026, 4));

            assertThat(primerCobro).isEqualTo(LocalDate.of(2026, 2, 28));
            assertThat(segundoCobro).isEqualTo(LocalDate.of(2026, 3, 31));
            assertThat(tercerCobro).isEqualTo(LocalDate.of(2026, 4, 30));
            assertThat(ancla.dayOfMonth()).isEqualTo(31);
        }

        @Test
        @DisplayName("la ventana encadenada recupera el 31 despues de haber cerrado en febrero")
        void la_ventana_encadenada_recupera_el_dia() {
            BillingAnchor ancla = BillingAnchor.from(LocalDate.of(2026, 1, 31));

            BillingCycleWindow enero = BillingCycleWindow.startingOn(LocalDate.of(2026, 1, 31),
                    ancla, BillingPeriodicity.MONTHLY);
            BillingCycleWindow febrero = BillingCycleWindow.startingOn(enero.nextBillingDate(),
                    ancla, BillingPeriodicity.MONTHLY);
            BillingCycleWindow marzo = BillingCycleWindow.startingOn(febrero.nextBillingDate(),
                    ancla, BillingPeriodicity.MONTHLY);

            assertThat(enero.nextBillingDate()).isEqualTo(LocalDate.of(2026, 2, 28));
            assertThat(febrero.nextBillingDate()).isEqualTo(LocalDate.of(2026, 3, 31));
            assertThat(marzo.nextBillingDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        }
    }

    @Nested
    @DisplayName("La ventana que toca cobrar")
    class Ventana {

        @Test
        @DisplayName("el periodo termina la vispera del proximo cobro, sin huecos ni solapes")
        void el_periodo_termina_la_vispera() {
            BillingCycleWindow ventana = BillingCycleWindow.startingOn(LocalDate.of(2026, 3, 2),
                    new BillingAnchor(2), BillingPeriodicity.MONTHLY);

            assertThat(ventana.period().start()).isEqualTo(LocalDate.of(2026, 3, 2));
            assertThat(ventana.period().end()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(ventana.nextBillingDate()).isEqualTo(LocalDate.of(2026, 4, 2));
        }

        @Test
        @DisplayName("un ciclo anual avanza doce meses y sobrevive al 29 de febrero")
        void ciclo_anual() {
            BillingCycleWindow ventana = BillingCycleWindow.startingOn(LocalDate.of(2028, 2, 29),
                    new BillingAnchor(29), BillingPeriodicity.ANNUAL);

            assertThat(ventana.period().end()).isEqualTo(LocalDate.of(2029, 2, 27));
            assertThat(ventana.nextBillingDate()).isEqualTo(LocalDate.of(2029, 2, 28));
        }

        @Test
        @DisplayName("un arranque desalineado avanza un mes mas en vez de nacer invertido")
        void arranque_desalineado() {
            // Ancla en el 2 pero el contrato empieza a devengar el 5: el ancla del mes
            // siguiente cae DESPUES, asi que no hay problema. El caso limite es el
            // contrario, y la guarda del while es la que lo cubre.
            BillingCycleWindow ventana = BillingCycleWindow.startingOn(LocalDate.of(2026, 3, 5),
                    new BillingAnchor(2), BillingPeriodicity.MONTHLY);

            assertThat(ventana.period().end()).isEqualTo(LocalDate.of(2026, 4, 1));
        }

        @Test
        @DisplayName("no admite una fecha de proximo cobro que no empalme con el periodo")
        void exige_que_empalme() {
            assertThatThrownBy(() -> new BillingCycleWindow(
                    new ServicePeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
                    LocalDate.of(2026, 4, 15))).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be the day after the period");
        }
    }
}
