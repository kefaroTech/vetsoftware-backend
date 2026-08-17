package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository.Overlap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Redaccion de los cruces segun el alcance de sede del caller.
 *
 * <p>
 * <b>Esta clase decide cuanto se le cuenta a quien.</b> El cruce se calcula por
 * empresa + veterinario —un vet no puede estar en dos consultas a la vez aunque
 * sean de sedes distintas—, pero el listado de citas esta acotado por sede: si
 * el 409 devolviera los ids de todos los cruces, un empleado de una sede podria
 * reconstruir la agenda de otra a base de peticiones. Eso era una fuga real.
 *
 * <p>
 * Es logica de redaccion, no de autorizacion: nunca deja pasar de mas, y ante
 * cualquier duda —lista nula, alcance vacio, cita sin sede— <b>falla
 * cerrado</b> y no revela nada. Por eso se prueba exhaustivamente aunque sean
 * veinte lineas.
 */
@DisplayName("AppointmentOverlaps — que cruces se le pueden contar al caller")
class AppointmentOverlapsTest {

    private static final Long MI_SEDE = 10L;
    private static final Long OTRA_MI_SEDE = 11L;
    private static final Long SEDE_AJENA = 99L;

    private static final Set<Long> MI_ALCANCE = Set.of(MI_SEDE, OTRA_MI_SEDE);

    private static final Overlap EN_MI_SEDE = new Overlap(1L, MI_SEDE);
    private static final Overlap EN_MI_OTRA_SEDE = new Overlap(2L, OTRA_MI_SEDE);
    private static final Overlap EN_SEDE_AJENA = new Overlap(3L, SEDE_AJENA);
    private static final Overlap SIN_SEDE = new Overlap(4L, null);

    @Nested
    @DisplayName("visibleIds — lo que se puede publicar en el 409")
    class VisibleIds {

        @Test
        @DisplayName("devuelve los cruces de las sedes del caller")
        void devuelve_los_cruces_de_las_sedes_del_caller() {
            assertThat(AppointmentOverlaps.visibleIds(List.of(EN_MI_SEDE, EN_MI_OTRA_SEDE),
                    MI_ALCANCE)).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("oculta por completo un cruce de otra sede")
        void oculta_por_completo_un_cruce_de_otra_sede() {
            // El caso de la fuga: hay conflicto y la operacion se bloquea, pero el
            // caller no puede saber con que.
            assertThat(AppointmentOverlaps.visibleIds(List.of(EN_SEDE_AJENA), MI_ALCANCE))
                    .isEmpty();
        }

        @Test
        @DisplayName("en el caso mixto solo salen los visibles, sin rastro de los demas")
        void en_el_caso_mixto_solo_salen_los_visibles() {
            List<Long> visibles = AppointmentOverlaps
                    .visibleIds(List.of(EN_MI_SEDE, EN_SEDE_AJENA, EN_MI_OTRA_SEDE), MI_ALCANCE);

            // Ni el id ajeno ni un hueco que delate su existencia: un recuento tambien
            // seria divulgacion.
            assertThat(visibles).containsExactly(1L, 2L).doesNotContain(3L).hasSize(2);
        }

        @Test
        @DisplayName("descarta un cruce sin sede: sin dato de sede no se puede autorizar a mostrarlo")
        void descarta_un_cruce_sin_sede() {
            assertThat(AppointmentOverlaps.visibleIds(List.of(SIN_SEDE), MI_ALCANCE)).isEmpty();
        }

        @Test
        @DisplayName("con alcance vacio no revela nada: falla cerrado")
        void con_alcance_vacio_no_revela_nada() {
            // currentBranchIdsOrEmpty() devuelve Set.of() cuando no hay contexto de
            // empleado. Esa ausencia tiene que significar "nada", no "todo".
            assertThat(AppointmentOverlaps.visibleIds(List.of(EN_MI_SEDE, EN_SEDE_AJENA), Set.of()))
                    .isEmpty();
        }

        @Test
        @DisplayName("con alcance nulo no revela nada: falla cerrado")
        void con_alcance_nulo_no_revela_nada() {
            assertThat(AppointmentOverlaps.visibleIds(List.of(EN_MI_SEDE), null)).isEmpty();
        }

        @Test
        @DisplayName("sin cruces devuelve la lista vacia, nunca null")
        void sin_cruces_devuelve_la_lista_vacia() {
            assertThat(AppointmentOverlaps.visibleIds(List.of(), MI_ALCANCE)).isEmpty();
            assertThat(AppointmentOverlaps.visibleIds(null, MI_ALCANCE)).isEmpty();
        }

        @Test
        @DisplayName("conserva el orden en que llegaron los cruces")
        void conserva_el_orden_en_que_llegaron() {
            assertThat(AppointmentOverlaps.visibleIds(List.of(EN_MI_OTRA_SEDE, EN_MI_SEDE),
                    MI_ALCANCE)).containsExactly(2L, 1L);
        }
    }

    @Nested
    @DisplayName("allIds — el conteo interno, que no sale al cliente")
    class AllIds {

        @Test
        @DisplayName("devuelve todos los cruces, tambien los de otras sedes")
        void devuelve_todos_los_cruces() {
            // Este es el dato con el que se decide SI se bloquea; el otro es el que se
            // puede contar. Confundirlos en cualquiera de los dos sentidos es un fallo:
            // bloquear de menos deja doble booking, publicar de mas es la fuga.
            assertThat(AppointmentOverlaps.allIds(List.of(EN_MI_SEDE, EN_SEDE_AJENA, SIN_SEDE)))
                    .containsExactly(1L, 3L, 4L);
        }

        @Test
        @DisplayName("sin cruces devuelve la lista vacia")
        void sin_cruces_devuelve_la_lista_vacia() {
            assertThat(AppointmentOverlaps.allIds(List.of())).isEmpty();
        }

        @Test
        @DisplayName("un cruce fuera de alcance bloquea aunque no se pueda nombrar")
        void un_cruce_fuera_de_alcance_bloquea_aunque_no_se_pueda_nombrar() {
            List<Overlap> soloAjenos = List.of(EN_SEDE_AJENA);

            assertThat(AppointmentOverlaps.allIds(soloAjenos)).hasSize(1);
            assertThat(AppointmentOverlaps.visibleIds(soloAjenos, MI_ALCANCE)).isEmpty();
        }
    }
}
