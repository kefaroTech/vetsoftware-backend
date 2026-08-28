package com.vetsoftware.app.publicholiday.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("HolidayCalendar — de aqui salen todos los plazos en dias habiles")
class HolidayCalendarTest {

    /**
     * Los 19 festivos observados de 2026, tal como los siembra el changeset 360.
     */
    private static final Set<LocalDate> FESTIVOS_2026 = Set.of(LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 12), LocalDate.of(2026, 3, 23), LocalDate.of(2026, 4, 2),
            LocalDate.of(2026, 4, 3), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 18),
            LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 29),
            LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 7),
            LocalDate.of(2026, 8, 17), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 25));

    private static HolidayCalendar anio2026() {
        return new HolidayCalendar(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(2026), FESTIVOS_2026);
    }

    @Nested
    @DisplayName("El plazo de dias habiles")
    class ElPlazo {

        @Test
        @DisplayName("15 dias habiles desde el 1 de julio de 2026 vencen el 24, no el 16")
        void quince_dias_habiles_desde_el_1_de_julio_vencen_el_24() {
            LocalDate radicacion = LocalDate.of(2026, 7, 1);

            LocalDate vencimiento = anio2026().deadline(radicacion, 15);

            // Los dos festivos del camino son el 13 (Chiquinquira, trasladado por la Ley
            // 2578 de 2026) y el 20 (Independencia). Contando dias corridos el sistema
            // habria dicho el 16 de julio: OCHO dias antes de lo que la ley concede, y
            // el error cae siempre del lado de dar por vencido un plazo que sigue vivo.
            assertThat(vencimiento).isEqualTo(LocalDate.of(2026, 7, 24));
            assertThat(radicacion.plusDays(15)).isEqualTo(LocalDate.of(2026, 7, 16));
            assertThat(vencimiento).isAfter(radicacion.plusDays(15));
        }

        @Test
        @DisplayName("el dia de partida no cuenta: el plazo empieza al dia habil siguiente")
        void el_dia_de_partida_no_cuenta() {
            // Martes 7 de julio de 2026, dia habil corriente. Un dia habil de plazo
            // vence el miercoles 8, no el mismo martes: contar el dia del hecho
            // acortaria una jornada todos los plazos del producto.
            assertThat(anio2026().deadline(LocalDate.of(2026, 7, 7), 1))
                    .isEqualTo(LocalDate.of(2026, 7, 8));
        }

        @Test
        @DisplayName("un dia habil desde el viernes vence el lunes, saltando el fin de semana")
        void un_dia_habil_desde_el_viernes_vence_el_lunes() {
            assertThat(anio2026().deadline(LocalDate.of(2026, 7, 3), 1))
                    .isEqualTo(LocalDate.of(2026, 7, 6));
        }

        @Test
        @DisplayName("si el dia siguiente es festivo se salta: el 12 de octubre no cuenta")
        void un_festivo_no_cuenta_como_dia_habil() {
            // Viernes 9 de octubre de 2026; el lunes 12 es festivo, asi que el primer
            // dia habil es el martes 13.
            assertThat(anio2026().deadline(LocalDate.of(2026, 10, 9), 1))
                    .isEqualTo(LocalDate.of(2026, 10, 13));
        }

        @Test
        @DisplayName("los 10 dias habiles de la consulta de habeas data desde el 1 de junio")
        void diez_dias_habiles_desde_el_1_de_junio() {
            // Lunes 1 de junio de 2026. Por el camino, Corpus Christi (8) y Sagrado
            // Corazon (15), los dos lunes por traslado de la Ley 51 de 1983.
            assertThat(anio2026().deadline(LocalDate.of(2026, 6, 1), 10))
                    .isEqualTo(LocalDate.of(2026, 6, 17));
        }

        @ParameterizedTest
        @CsvSource({"2026-07-01, 15, 2026-07-24", "2026-07-07, 1, 2026-07-08",
                "2026-07-03, 1, 2026-07-06", "2026-10-09, 1, 2026-10-13",
                "2026-06-01, 10, 2026-06-17", "2026-12-01, 5, 2026-12-09"})
        @DisplayName("matriz de plazos contra el calendario real de 2026")
        void matriz_de_plazos(LocalDate desde, int diasHabiles, LocalDate esperado) {
            assertThat(anio2026().deadline(desde, diasHabiles)).isEqualTo(esperado);
        }
    }

    @Nested
    @DisplayName("Dos festivos observados el mismo dia")
    class DosFestivosElMismoDia {

        @Test
        @DisplayName("el 1 de julio de 2019 cuenta una vez, aunque fueran dos efemerides")
        void dos_efemerides_el_mismo_lunes_cuentan_una_vez() {
            // El Sagrado Corazon (Pascua+68) y San Pedro y San Pablo (29 de junio,
            // sabado, trasladado) cayeron los dos en el lunes 1 de julio de 2019. La
            // identidad del festivo es la fecha OBSERVADA, asi que el conjunto tiene una
            // sola entrada y el dia se descuenta una vez, no dos.
            HolidayCalendar julio2019 = new HolidayCalendar(LocalDate.of(2019, 6, 1),
                    LocalDate.of(2019, 7, 31), Set.of(2019), Set.of(LocalDate.of(2019, 7, 1)));

            assertThat(julio2019.deadline(LocalDate.of(2019, 6, 28), 1))
                    .isEqualTo(LocalDate.of(2019, 7, 2));
            assertThat(julio2019.weekdayHolidaysBetween(LocalDate.of(2019, 6, 28),
                    LocalDate.of(2019, 7, 2))).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Cobertura del calendario")
    class Cobertura {

        @Test
        @DisplayName("se niega a calcular si el plazo entra en un ano sin sembrar")
        void se_niega_si_el_ano_no_esta_sembrado() {
            // Un plazo de 15 dias habiles desde el 20 de diciembre cruza a enero. Si
            // 2027 no esta sembrado, sus 19 festivos NO estan en el conjunto y el
            // calculo los trataria como dias habiles: el vencimiento saldria MAS TARDE
            // que el real, es decir en la direccion de incumplir. Fallar es la unica
            // respuesta honesta.
            HolidayCalendar soloHasta2026 = new HolidayCalendar(LocalDate.of(2026, 1, 1),
                    LocalDate.of(2027, 3, 31), Set.of(2026), FESTIVOS_2026);

            assertThatThrownBy(() -> soloHasta2026.deadline(LocalDate.of(2026, 12, 20), 15))
                    .isInstanceOf(HolidayCalendarGapException.class)
                    .hasMessageContaining("does not cover 2027-01-01");
        }

        @Test
        @DisplayName("se niega a calcular si el recorrido se sale del tramo cargado")
        void se_niega_si_el_recorrido_sale_del_tramo() {
            HolidayCalendar tramoCorto = new HolidayCalendar(LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 10), Set.of(2026), FESTIVOS_2026);

            assertThatThrownBy(() -> tramoCorto.deadline(LocalDate.of(2026, 7, 1), 15))
                    .isInstanceOf(HolidayCalendarGapException.class);
        }

        @Test
        @DisplayName("isBusinessDay tambien se niega fuera del tramo, no responde true")
        void is_business_day_no_responde_optimista_fuera_del_tramo() {
            assertThatThrownBy(() -> anio2026().isBusinessDay(LocalDate.of(2027, 5, 3)))
                    .isInstanceOf(HolidayCalendarGapException.class);
        }
    }

    @Nested
    @DisplayName("Dias habiles y no habiles")
    class DiasHabiles {

        @ParameterizedTest
        @ValueSource(strings = {"2026-07-04", "2026-07-05", "2026-07-13", "2026-12-25"})
        @DisplayName("sabado, domingo y festivo no son habiles")
        void no_son_habiles(String fecha) {
            assertThat(anio2026().isBusinessDay(LocalDate.parse(fecha))).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-07-01", "2026-07-14", "2026-12-24", "2026-12-31"})
        @DisplayName("un dia de semana que no es festivo si es habil")
        void si_son_habiles(String fecha) {
            assertThat(anio2026().isBusinessDay(LocalDate.parse(fecha))).isTrue();
        }

        @Test
        @DisplayName("cuenta solo los festivos entre semana: el 25 de diciembre de 2026 es viernes")
        void cuenta_los_festivos_entre_semana() {
            assertThat(anio2026().weekdayHolidaysBetween(LocalDate.of(2026, 12, 20),
                    LocalDate.of(2026, 12, 31))).isEqualTo(1);
        }

        @Test
        @DisplayName("no cuenta nada si la fecha de vencimiento no es posterior al inicio")
        void no_cuenta_nada_si_el_rango_esta_invertido() {
            assertThat(anio2026().weekdayHolidaysBetween(LocalDate.of(2026, 7, 20),
                    LocalDate.of(2026, 7, 1))).isZero();
        }
    }

    @Nested
    @DisplayName("Invariantes")
    class Invariantes {

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -15})
        @DisplayName("un plazo de cero o negativo no es un plazo")
        void rechaza_plazos_no_positivos(int diasHabiles) {
            assertThatThrownBy(() -> anio2026().deadline(LocalDate.of(2026, 7, 1), diasHabiles))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("businessDays must be at least 1");
        }

        @Test
        @DisplayName("sin fecha de partida no hay plazo que calcular")
        void rechaza_el_inicio_nulo() {
            assertThatThrownBy(() -> anio2026().deadline(null, 15))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("start is required");
        }

        @Test
        @DisplayName("el tramo cubierto no puede estar del reves")
        void rechaza_el_tramo_invertido() {
            assertThatThrownBy(() -> new HolidayCalendar(LocalDate.of(2026, 12, 31),
                    LocalDate.of(2026, 1, 1), Set.of(2026), FESTIVOS_2026))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("coveredFrom cannot be after coveredTo");
        }

        @Test
        @DisplayName("el conjunto de festivos que guarda es inmutable")
        void el_conjunto_es_inmutable() {
            Set<LocalDate> festivos = anio2026().observedHolidays();

            assertThatThrownBy(() -> festivos.add(LocalDate.of(2026, 7, 15)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
