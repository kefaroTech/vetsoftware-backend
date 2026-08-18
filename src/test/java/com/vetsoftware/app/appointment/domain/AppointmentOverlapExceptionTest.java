package com.vetsoftware.app.appointment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Las 16 ramas de esta clase deciden <b>qué se le revela al caller</b> cuando
 * su agenda se cruza con la de otro veterinario: con citas visibles (mismas
 * sedes que el caller ve) el mensaje nombra al veterinario y las citas
 * concretas; sin citas visibles (el cruce es de una sede ajena) el mensaje es
 * genérico y no expone ni el id, ni el nombre, ni el conteo real. Es redacción
 * de datos sensibles, no un detalle de formato — de ahí el test dedicado.
 */
@DisplayName("AppointmentOverlapException — redaccion del mensaje segun sede visible")
class AppointmentOverlapExceptionTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 8, 20, 9, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 8, 20, 9, 30);
    private static final String FORCE_HINT = " Si tienes permiso para forzar el agendamiento,"
            + " vuelve a enviarla marcando \"agendar de todos modos\".";

    @Nested
    @DisplayName("sin citas visibles: la cita en conflicto es de una sede que el caller no ve")
    class SinCitasVisibles {

        @Test
        @DisplayName("no revela el id, ni el nombre del veterinario, ni el conteo real de cruces")
        void redaccion_generica_sin_revelar_nada() {
            AppointmentOverlapException ex = new AppointmentOverlapException(4L, "Dra. Vet", INICIO,
                    FIN, List.of(), 3);

            assertThat(ex.getMessage()).isEqualTo("El veterinario seleccionado ya está ocupado el"
                    + " 20/08/2026 de 09:00 a 09:30 con una cita de otra sede." + FORCE_HINT);
            assertThat(ex.getMessage()).doesNotContain("Dra. Vet");
        }

        @Test
        @DisplayName("una lista visible null se trata igual que vacia: nunca expone null")
        void lista_null_se_trata_como_vacia() {
            AppointmentOverlapException ex = new AppointmentOverlapException(4L, "Dra. Vet", INICIO,
                    FIN, null, 1);

            assertThat(ex.getOverlappingAppointmentIds()).isEmpty();
            assertThat(ex.getMessage()).contains("con una cita de otra sede.");
        }

        @Test
        @DisplayName("getOverlappingAppointmentIds queda vacio para el caller")
        void los_ids_visibles_quedan_vacios() {
            AppointmentOverlapException ex = new AppointmentOverlapException(4L, "Dra. Vet", INICIO,
                    FIN, List.of(), 3);

            assertThat(ex.getOverlappingAppointmentIds()).isEmpty();
        }

        @Test
        @DisplayName("el id del empleado y el conteo total se conservan para el log, aunque no salgan al caller")
        void el_id_de_empleado_y_el_conteo_se_conservan_para_el_log() {
            AppointmentOverlapException ex = new AppointmentOverlapException(4L, "Dra. Vet", INICIO,
                    FIN, List.of(), 5);

            assertThat(ex.getEmployeeId()).isEqualTo(4L);
            assertThat(ex.getOverlapCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("con citas visibles: mismas sedes que el caller tiene asignadas")
    class ConCitasVisibles {

        @Test
        @DisplayName("una sola cita visible: nombra al veterinario y la cita, en singular")
        void una_cita_visible_se_nombra_en_singular() {
            AppointmentOverlapException ex = new AppointmentOverlapException(4L, "Dra. Vet", INICIO,
                    FIN, List.of(70L), 1);

            assertThat(ex.getMessage())
                    .isEqualTo("Dra. Vet ya tiene otra cita que se cruza con el 20/08/2026 de 09:00"
                            + " a 09:30 (cita 70)." + FORCE_HINT);
            assertThat(ex.getOverlappingAppointmentIds()).containsExactly(70L);
        }

        @Test
        @DisplayName("varias citas visibles: las enumera separadas por coma, en plural")
        void varias_citas_visibles_se_enumeran_en_plural() {
            AppointmentOverlapException ex = new AppointmentOverlapException(4L, "Dra. Vet", INICIO,
                    FIN, List.of(70L, 71L, 72L), 3);

            assertThat(ex.getMessage()).contains("(citas 70, 71, 72)");
            assertThat(ex.getOverlappingAppointmentIds()).containsExactly(70L, 71L, 72L);
        }

        @Test
        @DisplayName("sin nombre de veterinario cae al generico 'El veterinario seleccionado'")
        void sin_nombre_de_veterinario_cae_al_generico() {
            AppointmentOverlapException ex = new AppointmentOverlapException(4L, null, INICIO, FIN,
                    List.of(70L), 1);

            assertThat(ex.getMessage())
                    .startsWith("El veterinario seleccionado ya tiene otra cita");
        }

        @Test
        @DisplayName("un nombre de veterinario en blanco tambien cae al generico")
        void nombre_de_veterinario_en_blanco_cae_al_generico() {
            AppointmentOverlapException ex = new AppointmentOverlapException(4L, "   ", INICIO, FIN,
                    List.of(70L), 1);

            assertThat(ex.getMessage())
                    .startsWith("El veterinario seleccionado ya tiene otra cita");
        }

        @Test
        @DisplayName("la lista expuesta es una copia inmutable: mutar la lista original no la afecta")
        void la_lista_expuesta_es_una_copia_inmutable() {
            List<Long> mutable = new ArrayList<>(List.of(70L));
            AppointmentOverlapException ex = new AppointmentOverlapException(4L, "Dra. Vet", INICIO,
                    FIN, mutable, 1);

            mutable.add(99L);

            assertThat(ex.getOverlappingAppointmentIds()).containsExactly(70L);
        }
    }

    @Nested
    @DisplayName("datos de horario ausentes")
    class HorarioAusente {

        @Test
        @DisplayName("startAt null cae al texto generico 'el horario solicitado'")
        void startAt_null_usa_el_horario_solicitado() {
            AppointmentOverlapException ex = new AppointmentOverlapException(4L, "Dra. Vet", null,
                    FIN, List.of(70L), 1);

            assertThat(ex.getMessage()).contains("se cruza con el horario solicitado (cita 70)");
        }

        @Test
        @DisplayName("endAt null deja el cierre del rango marcado con signo de interrogacion")
        void endAt_null_deja_el_cierre_con_interrogacion() {
            AppointmentOverlapException ex = new AppointmentOverlapException(4L, "Dra. Vet", INICIO,
                    null, List.of(70L), 1);

            assertThat(ex.getMessage()).contains("de 09:00 a ?");
        }

        @Test
        @DisplayName("startAt y endAt ausentes en la redaccion generica tambien usan el horario solicitado")
        void startAt_null_en_la_redaccion_generica() {
            AppointmentOverlapException ex = new AppointmentOverlapException(4L, "Dra. Vet", null,
                    null, List.of(), 1);

            assertThat(ex.getMessage())
                    .isEqualTo("El veterinario seleccionado ya está ocupado el horario solicitado"
                            + " con una cita de otra sede." + FORCE_HINT);
        }
    }
}
