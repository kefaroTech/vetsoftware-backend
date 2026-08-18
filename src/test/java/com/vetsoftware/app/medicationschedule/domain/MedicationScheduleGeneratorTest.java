package com.vetsoftware.app.medicationschedule.domain;

import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.EMPLEADO;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.PRIMERA_TOMA;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.cada8hDurante3Dias;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.orden;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.sinFechaDeInicio;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.sinHoraDeInicio;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El generador decide a que hora recibe cada dosis un paciente hospitalizado.
 * Una toma de mas o de menos aqui es una dosis de mas o de menos en la vena de
 * un animal, asi que lo que se prueba es la cuenta y el reloj, no la forma.
 */
@DisplayName("MedicationScheduleGenerator — cuantas tomas y a que hora")
class MedicationScheduleGeneratorTest {

    private static List<LocalDateTime> horasDe(List<MedicationSchedule> tomas) {
        return tomas.stream().map(MedicationSchedule::getCurrentDateTime).toList();
    }

    @Nested
    @DisplayName("generate — el calendario inicial")
    class Generacion {

        @Test
        @DisplayName("cada 8 horas durante 3 dias son nueve tomas, cada una 8 horas despues")
        void cada_8_horas_durante_3_dias_son_nueve_tomas() {
            List<MedicationSchedule> tomas = MedicationScheduleGenerator
                    .generate(cada8hDurante3Dias(), EMPLEADO);

            assertThat(tomas).hasSize(9);
            assertThat(horasDe(tomas).subList(0, 4)).containsExactly(PRIMERA_TOMA,
                    PRIMERA_TOMA.plusHours(8), PRIMERA_TOMA.plusHours(16),
                    PRIMERA_TOMA.plusHours(24));
            assertThat(horasDe(tomas).getLast()).isEqualTo(PRIMERA_TOMA.plusHours(64));
        }

        @ParameterizedTest(name = "{0} durante {1} dias → {2} tomas")
        @CsvSource({"EVERY_4H, 1, 6", "EVERY_6H, 1, 4", "EVERY_8H, 1, 3", "EVERY_12H, 1, 2",
                "EVERY_24H, 1, 1", "EVERY_8H, 3, 9", "EVERY_12H, 7, 14"})
        @DisplayName("la cuenta sale de las horas del dia partidas por el intervalo")
        void la_cuenta_sale_de_las_horas_partidas_por_el_intervalo(String frecuencia, int dias,
                int esperadas) {
            assertThat(MedicationScheduleGenerator
                    .generate(orden(frecuencia, "FIXED", "DAYS", dias), EMPLEADO))
                    .hasSize(esperadas);
        }

        @Test
        @DisplayName("una pauta por numero de dosis genera exactamente esas")
        void una_pauta_por_dosis_genera_exactamente_esas() {
            // DOSES manda sobre el calculo por dias: el veterinario pidio 5 tomas.
            assertThat(MedicationScheduleGenerator.generate(orden("EVERY_8H", "FIXED", "DOSES", 5),
                    EMPLEADO)).hasSize(5);
        }

        @Test
        @DisplayName("una toma unica se agenda una sola vez")
        void una_toma_unica_se_agenda_una_sola_vez() {
            List<MedicationSchedule> tomas = MedicationScheduleGenerator
                    .generate(orden("SINGLE", "FIXED", "DOSES", 5), EMPLEADO);

            // SINGLE ignora la duracion: es una dosis y punto.
            assertThat(tomas).hasSize(1);
            assertThat(horasDe(tomas)).containsExactly(PRIMERA_TOMA);
        }

        @Test
        @DisplayName("una infusion continua no genera tomas: no hay horas que marcar")
        void una_infusion_continua_no_genera_tomas() {
            // El goteo continuo se controla por bomba, no por calendario de tomas.
            assertThat(MedicationScheduleGenerator.generate(orden("CONTINUOUS", "FIXED", "DAYS", 3),
                    EMPLEADO)).isEmpty();
        }

        @ParameterizedTest(name = "frecuencia [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"EVERY_5H", "PRN", "cuando duela"})
        @DisplayName("una frecuencia que no es discreta no se agenda")
        void una_frecuencia_no_discreta_no_se_agenda(String frecuencia) {
            // Mejor no agendar nada que inventar un intervalo: la enfermera veria horas
            // que nadie prescribio.
            assertThat(MedicationScheduleGenerator.generate(orden(frecuencia, "FIXED", "DAYS", 3),
                    EMPLEADO)).isEmpty();
        }

        @Test
        @DisplayName("la frecuencia se reconoce sin importar mayusculas ni espacios")
        void la_frecuencia_se_reconoce_sin_importar_el_formato() {
            assertThat(MedicationScheduleGenerator
                    .generate(orden("  every_8h  ", "FIXED", "DAYS", 1), EMPLEADO)).hasSize(3);
        }

        @Test
        @DisplayName("sin fecha de inicio no se agenda nada")
        void sin_fecha_de_inicio_no_se_agenda_nada() {
            assertThat(MedicationScheduleGenerator.generate(sinFechaDeInicio(), EMPLEADO))
                    .isEmpty();
        }

        @Test
        @DisplayName("sin hora de inicio se arranca a las ocho de la manana")
        void sin_hora_de_inicio_se_arranca_a_las_ocho() {
            List<MedicationSchedule> tomas = MedicationScheduleGenerator
                    .generate(sinHoraDeInicio("EVERY_24H"), EMPLEADO);

            assertThat(horasDe(tomas)).containsExactly(PRIMERA_TOMA);
        }
    }

    @Nested
    @DisplayName("generate — los topes que evitan un calendario absurdo")
    class Topes {

        @Test
        @DisplayName("una pauta indefinida se acota a catorce dias")
        void una_pauta_indefinida_se_acota_a_catorce_dias() {
            // Sin horizonte, una orden indefinida generaria filas hasta el infinito.
            assertThat(MedicationScheduleGenerator
                    .generate(orden("EVERY_8H", "FIXED", "INDEFINITE", null), EMPLEADO))
                    .hasSize(42);
        }

        @Test
        @DisplayName("sin duracion tambien cae al horizonte de catorce dias")
        void sin_duracion_cae_al_horizonte() {
            assertThat(MedicationScheduleGenerator.generate(orden("EVERY_24H", "FIXED", null, null),
                    EMPLEADO)).hasSize(14);
        }

        @Test
        @DisplayName("una cantidad de cero o negativa no cuenta como duracion")
        void una_cantidad_no_positiva_no_cuenta_como_duracion() {
            // Cae al horizonte por defecto en vez de generar cero tomas, que dejaria la
            // orden sin agenda y sin aviso.
            assertThat(MedicationScheduleGenerator.generate(orden("EVERY_24H", "FIXED", "DAYS", 0),
                    EMPLEADO)).hasSize(14);
            assertThat(MedicationScheduleGenerator
                    .generate(orden("EVERY_24H", "FIXED", "DOSES", -3), EMPLEADO)).hasSize(14);
        }

        @Test
        @DisplayName("nunca se pasa de noventa tomas por larga que sea la orden")
        void nunca_se_pasa_de_noventa_tomas() {
            // 30 dias cada 4 horas serian 180: el tope corta en 90.
            assertThat(MedicationScheduleGenerator.generate(orden("EVERY_4H", "FIXED", "DAYS", 30),
                    EMPLEADO)).hasSize(90);
            assertThat(MedicationScheduleGenerator
                    .generate(orden("EVERY_8H", "FIXED", "DOSES", 500), EMPLEADO)).hasSize(90);
        }

        @Test
        @DisplayName("una duracion mas corta que el intervalo deja al menos una toma")
        void una_duracion_mas_corta_que_el_intervalo_deja_una_toma() {
            // 1 dia cada 48 horas daria cero por division entera; el minimo de uno evita
            // que la orden quede sin ninguna toma agendada.
            assertThat(MedicationScheduleGenerator.generate(orden("EVERY_24H", "FIXED", "DAYS", 1),
                    EMPLEADO)).hasSize(1);
        }

        @Test
        @DisplayName("DOSES sin cantidad especificada cae al horizonte, no revienta")
        void doses_sin_cantidad_cae_al_horizonte() {
            // durationQuantity=null corta el "&&" del primer if antes de mirar el
            // signo: sin este caso esa rama del cortocircuito no la ejercita nadie.
            assertThat(MedicationScheduleGenerator
                    .generate(orden("EVERY_24H", "FIXED", "DOSES", null), EMPLEADO)).hasSize(14);
        }

        @Test
        @DisplayName("DAYS sin cantidad especificada cae al horizonte, no revienta")
        void days_sin_cantidad_cae_al_horizonte() {
            assertThat(MedicationScheduleGenerator
                    .generate(orden("EVERY_24H", "FIXED", "DAYS", null), EMPLEADO)).hasSize(14);
        }
    }

    @Nested
    @DisplayName("generatePending — regenerar sin tocar lo ya aplicado")
    class Regeneracion {

        @Test
        @DisplayName("con dos aplicadas quedan siete pendientes de las nueve")
        void con_dos_aplicadas_quedan_siete_pendientes() {
            List<MedicationSchedule> pendientes = MedicationScheduleGenerator
                    .generatePending(cada8hDurante3Dias(), 2, null, EMPLEADO);

            assertThat(pendientes).hasSize(7);
        }

        @Test
        @DisplayName("en pauta fija se conservan las horas de reloj y se saltan las cubiertas")
        void en_pauta_fija_se_conservan_las_horas_de_reloj() {
            List<MedicationSchedule> pendientes = MedicationScheduleGenerator.generatePending(
                    cada8hDurante3Dias(), 2, LocalDateTime.of(2026, 1, 15, 17, 45), EMPLEADO);

            // Pauta FIJA: aunque la segunda toma se diera tarde (17:45 en vez de 16:00),
            // la tercera sigue tocando a su hora de reloj. Es lo que quiere el pabellon
            // cuando la pauta es "8, 16 y 24".
            assertThat(horasDe(pendientes).getFirst()).isEqualTo(PRIMERA_TOMA.plusHours(16));
            assertThat(horasDe(pendientes).getLast()).isEqualTo(PRIMERA_TOMA.plusHours(64));
        }

        @Test
        @DisplayName("en pauta por intervalo se re-cronometra desde la hora real de la ultima")
        void en_pauta_por_intervalo_se_recronometra_desde_la_hora_real() {
            LocalDateTime seAplicoTarde = LocalDateTime.of(2026, 1, 15, 17, 45);

            List<MedicationSchedule> pendientes = MedicationScheduleGenerator.generatePending(
                    orden("EVERY_8H", "INTERVAL", "DAYS", 3), 2, seAplicoTarde, EMPLEADO);

            // Pauta INTERVALO: lo que importa es que pasen 8 horas entre dosis, no la
            // hora del reloj. Si se aplico a las 17:45, la siguiente es a las 01:45, no
            // a las 00:00: adelantarla acortaria el intervalo y podria intoxicar.
            assertThat(horasDe(pendientes).getFirst())
                    .isEqualTo(LocalDateTime.of(2026, 1, 16, 1, 45));
            assertThat(horasDe(pendientes).get(1)).isEqualTo(LocalDateTime.of(2026, 1, 16, 9, 45));
            assertThat(pendientes).hasSize(7);
        }

        @Test
        @DisplayName("en pauta por intervalo sin ninguna aplicada se usan las horas de reloj")
        void en_intervalo_sin_aplicadas_se_usan_las_horas_de_reloj() {
            List<MedicationSchedule> pendientes = MedicationScheduleGenerator
                    .generatePending(orden("EVERY_8H", "INTERVAL", "DAYS", 3), 0, null, EMPLEADO);

            // No hay hora real de la que colgarse todavia: se parte del inicio previsto.
            assertThat(horasDe(pendientes).getFirst()).isEqualTo(PRIMERA_TOMA);
            assertThat(pendientes).hasSize(9);
        }

        @Test
        @DisplayName("si ya se aplicaron todas no queda nada pendiente")
        void si_ya_se_aplicaron_todas_no_queda_nada() {
            assertThat(MedicationScheduleGenerator.generatePending(cada8hDurante3Dias(), 9, null,
                    EMPLEADO)).isEmpty();
        }

        @Test
        @DisplayName("mas aplicadas que el total no genera un numero negativo de tomas")
        void mas_aplicadas_que_el_total_no_genera_negativos() {
            // Puede pasar si la orden se acorta despues de haber aplicado varias: el
            // maximo con cero es lo que evita un for con limite negativo.
            assertThat(MedicationScheduleGenerator.generatePending(cada8hDurante3Dias(), 20, null,
                    EMPLEADO)).isEmpty();
        }

        @Test
        @DisplayName("una toma unica ya aplicada no se vuelve a agendar")
        void una_toma_unica_ya_aplicada_no_se_reagenda() {
            assertThat(MedicationScheduleGenerator
                    .generatePending(orden("SINGLE", "FIXED", "DOSES", 1), 1, null, EMPLEADO))
                    .isEmpty();
            assertThat(MedicationScheduleGenerator
                    .generatePending(orden("SINGLE", "FIXED", "DOSES", 1), 0, null, EMPLEADO))
                    .hasSize(1);
        }

        @Test
        @DisplayName("sin hora de inicio tambien arranca a las ocho de la manana")
        void sin_hora_de_inicio_tambien_arranca_a_las_ocho() {
            // El mismo default que generate(), pero es una linea propia de
            // generatePending: sin este caso queda sin ejercitar.
            List<MedicationSchedule> pendientes = MedicationScheduleGenerator
                    .generatePending(sinHoraDeInicio("EVERY_24H"), 0, null, EMPLEADO);

            assertThat(horasDe(pendientes)).containsExactly(PRIMERA_TOMA);
        }

        @Test
        @DisplayName("una infusion continua tampoco genera pendientes")
        void una_infusion_continua_tampoco_genera_pendientes() {
            assertThat(MedicationScheduleGenerator
                    .generatePending(orden("CONTINUOUS", "FIXED", "DAYS", 3), 0, null, EMPLEADO))
                    .isEmpty();
        }

        @Test
        @DisplayName("sin fecha de inicio no se regenera nada")
        void sin_fecha_de_inicio_no_se_regenera_nada() {
            assertThat(MedicationScheduleGenerator.generatePending(sinFechaDeInicio(), 0, null,
                    EMPLEADO)).isEmpty();
        }

        @Test
        @DisplayName("una frecuencia desconocida no regenera nada")
        void una_frecuencia_desconocida_no_regenera_nada() {
            assertThat(MedicationScheduleGenerator.generatePending(orden("PRN", "FIXED", "DAYS", 3),
                    0, null, EMPLEADO)).isEmpty();
        }

        @Test
        @DisplayName("las tomas regeneradas nacen pendientes y sin marca de reprogramada")
        void las_tomas_regeneradas_nacen_pendientes() {
            MedicationSchedule primera = MedicationScheduleGenerator
                    .generatePending(cada8hDurante3Dias(), 2, null, EMPLEADO).getFirst();

            assertThat(primera.getAppliedStatus()).isEqualTo(AppliedStatus.PENDING);
            assertThat(primera.getRescheduled()).isFalse();
            assertThat(primera.getRealDateTime()).isNull();
            assertThat(primera.getHospitalizationMedication().name())
                    .isEqualTo("Amoxicilina 500mg");
            assertThat(primera.getCreatedBy()).isEqualTo(EMPLEADO);
        }
    }

    @Nested
    @DisplayName("intervalHours — la tabla de frecuencias")
    class TablaDeFrecuencias {

        @ParameterizedTest(name = "{0} → {1} h")
        @CsvSource({"EVERY_4H, 4", "EVERY_6H, 6", "EVERY_8H, 8", "EVERY_12H, 12", "EVERY_24H, 24"})
        @DisplayName("cada frecuencia discreta tiene sus horas")
        void cada_frecuencia_discreta_tiene_sus_horas(String frecuencia, int horas) {
            assertThat(MedicationScheduleGenerator.intervalHours(frecuencia)).isEqualTo(horas);
        }

        @ParameterizedTest(name = "frecuencia [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"CONTINUOUS", "SINGLE", "EVERY_5H", "  "})
        @DisplayName("lo que no es discreto devuelve null, no cero")
        void lo_que_no_es_discreto_devuelve_null(String frecuencia) {
            // Cero seria un intervalo valido y provocaria un bucle infinito de tomas a
            // la misma hora; null obliga a quien llama a decidir.
            assertThat(MedicationScheduleGenerator.intervalHours(frecuencia)).isNull();
        }

        @Test
        @DisplayName("tolera minusculas y espacios")
        void tolera_minusculas_y_espacios() {
            assertThat(MedicationScheduleGenerator.intervalHours("  every_12h ")).isEqualTo(12);
        }
    }
}
