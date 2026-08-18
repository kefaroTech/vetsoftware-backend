package com.vetsoftware.app.appointment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * La matriz de transiciones se reescribe aqui a proposito: es la
 * especificacion, no una copia de la implementacion. Si alguien anade un estado
 * o abre un salto nuevo en {@link AppointmentStatus} sin actualizarla, la
 * matriz completa (7x7) lo detecta.
 */
@DisplayName("AppointmentStatus — matriz completa de transiciones")
class AppointmentStatusTest {

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ESPERADAS = Map.of(
            AppointmentStatus.REQUESTED, EnumSet.of(AppointmentStatus.CONFIRMED,
                    AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
            AppointmentStatus.CONFIRMED, EnumSet.of(AppointmentStatus.ARRIVED,
                    AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
            AppointmentStatus.ARRIVED,
            EnumSet.of(AppointmentStatus.IN_PROGRESS, AppointmentStatus.CANCELLED,
                    AppointmentStatus.NO_SHOW),
            AppointmentStatus.IN_PROGRESS,
            EnumSet.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED),
            AppointmentStatus.COMPLETED, EnumSet.noneOf(AppointmentStatus.class),
            AppointmentStatus.NO_SHOW, EnumSet.noneOf(AppointmentStatus.class),
            AppointmentStatus.CANCELLED, EnumSet.noneOf(AppointmentStatus.class));

    static Stream<Arguments> matrizCompleta() {
        return Arrays.stream(AppointmentStatus.values())
                .flatMap(from -> Arrays.stream(AppointmentStatus.values())
                        .map(to -> arguments(from, to, ESPERADAS.get(from).contains(to))));
    }

    @ParameterizedTest(name = "{0} -> {1} permitida={2}")
    @MethodSource("matrizCompleta")
    @DisplayName("cada par origen-destino se permite o se niega segun la matriz de negocio")
    void cada_par_origen_destino_respeta_la_matriz(AppointmentStatus origen,
            AppointmentStatus destino, boolean permitida) {
        assertThat(origen.canTransitionTo(destino)).isEqualTo(permitida);
    }

    @ParameterizedTest
    @EnumSource(AppointmentStatus.class)
    @DisplayName("ningun estado admite transicionar a null")
    void ningun_estado_admite_transicionar_a_null(AppointmentStatus origen) {
        assertThat(origen.canTransitionTo(null)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "NO_SHOW", "CANCELLED"})
    @DisplayName("completada, no asistio y cancelada son estados terminales")
    void los_estados_finales_son_terminales(AppointmentStatus terminal) {
        assertThat(terminal.isTerminal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "NO_SHOW",
            "CANCELLED"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("los estados del flujo activo no son terminales")
    void los_estados_del_flujo_activo_no_son_terminales(AppointmentStatus enCurso) {
        assertThat(enCurso.isTerminal()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"REQUESTED", "CONFIRMED", "ARRIVED",
            "IN_PROGRESS"})
    @DisplayName("toda cita viva se puede cancelar")
    void toda_cita_viva_se_puede_cancelar(AppointmentStatus viva) {
        assertThat(viva.canTransitionTo(AppointmentStatus.CANCELLED)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(AppointmentStatus.class)
    @DisplayName("ningun estado puede volver a REQUESTED: la agenda no retrocede")
    void ningun_estado_puede_volver_a_requested(AppointmentStatus origen) {
        assertThat(origen.canTransitionTo(AppointmentStatus.REQUESTED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CANCELLED", "NO_SHOW"})
    @DisplayName("cancelada y no-asistio no ocupan la agenda del veterinario")
    void cancelada_y_no_asistio_no_ocupan_agenda(AppointmentStatus libera) {
        assertThat(libera.occupiesSchedule()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CANCELLED",
            "NO_SHOW"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("todos los demas estados, incluida COMPLETED, si ocupan la agenda")
    void los_demas_estados_ocupan_agenda(AppointmentStatus ocupa) {
        // COMPLETED es terminal pero sí ocupó el hueco: no es lo mismo que
        // isTerminal().
        assertThat(ocupa.occupiesSchedule()).isTrue();
    }

    @Test
    @DisplayName("namesNotOccupyingSchedule expone exactamente CANCELLED y NO_SHOW por nombre")
    void names_not_occupying_schedule_expone_cancelled_y_no_show() {
        assertThat(AppointmentStatus.namesNotOccupyingSchedule())
                .containsExactlyInAnyOrder("CANCELLED", "NO_SHOW");
    }
}
