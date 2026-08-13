package com.vetsoftware.app.procedureschedule.domain;

import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.EMPLEADO;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.ORDEN;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.PRIMERA_TOMA;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.SCHEDULE_ID;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.tomaPendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("ProcedureSchedule — una toma y su ciclo de vida")
class ProcedureScheduleTest {

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("sin orden de medicacion",
                            (ThrowingCallable) () -> ProcedureSchedule.create(null, PRIMERA_TOMA,
                                    PRIMERA_TOMA, AppliedStatus.PENDING, false, EMPLEADO),
                            "hospitalizationProcedure is required"),
                    arguments("sin hora prevista",
                            (ThrowingCallable) () -> ProcedureSchedule.create(ORDEN, null, null,
                                    AppliedStatus.PENDING, false, EMPLEADO),
                            "originalDateTime is required"),
                    arguments("sin autor",
                            (ThrowingCallable) () -> ProcedureSchedule.create(ORDEN, PRIMERA_TOMA,
                                    PRIMERA_TOMA, AppliedStatus.PENDING, false, null),
                            "createdBy is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza")
        void rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }

    @Nested
    @DisplayName("create")
    class Creacion {

        @Test
        @DisplayName("nace sin id, habilitada, pendiente y sin hora real")
        void nace_pendiente_y_sin_hora_real() {
            ProcedureSchedule toma = ProcedureSchedule.create(ORDEN, PRIMERA_TOMA, PRIMERA_TOMA,
                    AppliedStatus.PENDING, false, EMPLEADO);

            assertThat(toma.getId()).isNull();
            assertThat(toma.isEnabled()).isTrue();
            assertThat(toma.getAppliedStatus()).isEqualTo(AppliedStatus.PENDING);
            assertThat(toma.getRealDateTime()).as("todavia no se ha aplicado").isNull();
            assertThat(toma.getOriginalDateTime()).isEqualTo(PRIMERA_TOMA);
            assertThat(toma.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA);
            // createdDate lo pone LocalDateTime.now() dentro del factory. Deuda anotada
            // en "Determinismo" del CLAUDE.md.
            assertThat(toma.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("apply — marcar la toma como aplicada")
    class Aplicacion {

        @Test
        @DisplayName("guarda la hora real y la deja aplicada")
        void guarda_la_hora_real_y_la_deja_aplicada() {
            ProcedureSchedule toma = tomaPendiente();
            LocalDateTime seAplico = LocalDateTime.of(2026, 1, 15, 8, 20);

            toma.apply(seAplico);

            assertThat(toma.getAppliedStatus()).isEqualTo(AppliedStatus.APPLIED);
            assertThat(toma.getRealDateTime()).isEqualTo(seAplico);
        }

        @Test
        @DisplayName("la hora prevista no se toca al aplicar")
        void la_hora_prevista_no_se_toca_al_aplicar() {
            ProcedureSchedule toma = tomaPendiente();

            toma.apply(LocalDateTime.of(2026, 1, 15, 8, 20));

            // Prevista y real son dos datos distintos: la diferencia entre las dos es lo
            // que audita si la pauta se esta cumpliendo.
            assertThat(toma.getOriginalDateTime()).isEqualTo(PRIMERA_TOMA);
            assertThat(toma.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA);
        }

        @Test
        @DisplayName("aplicar sin hora real no se permite")
        void aplicar_sin_hora_real_no_se_permite() {
            ProcedureSchedule toma = tomaPendiente();

            assertThatThrownBy(() -> toma.apply(null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("realDateTime is required");
            assertThat(toma.getAppliedStatus()).as("un intento fallido no aplica nada")
                    .isEqualTo(AppliedStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("reschedule — mover la hora")
    class Reprogramacion {

        @Test
        @DisplayName("mueve la hora actual y deja la marca de reprogramada")
        void mueve_la_hora_y_marca_reprogramada() {
            ProcedureSchedule toma = tomaPendiente();
            LocalDateTime nueva = LocalDateTime.of(2026, 1, 15, 10, 0);

            toma.reschedule(nueva);

            assertThat(toma.getCurrentDateTime()).isEqualTo(nueva);
            assertThat(toma.getRescheduled()).isTrue();
        }

        @Test
        @DisplayName("la hora original se conserva: es el rastro de lo que se prescribio")
        void la_hora_original_se_conserva() {
            ProcedureSchedule toma = tomaPendiente();

            toma.reschedule(LocalDateTime.of(2026, 1, 15, 10, 0));

            // Perder la original borraria la evidencia de que la pauta se movio.
            assertThat(toma.getOriginalDateTime()).isEqualTo(PRIMERA_TOMA);
        }

        @Test
        @DisplayName("reprogramar sin hora nueva no se permite")
        void reprogramar_sin_hora_nueva_no_se_permite() {
            ProcedureSchedule toma = tomaPendiente();

            assertThatThrownBy(() -> toma.reschedule(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currentDateTime is required");
            assertThat(toma.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("deshabilitar y volver a habilitar no toca el resto de la toma")
        void deshabilitar_y_habilitar_no_toca_el_resto() {
            ProcedureSchedule toma = tomaPendiente();

            toma.disable();
            assertThat(toma.isEnabled()).isFalse();

            toma.enable();
            assertThat(toma.isEnabled()).isTrue();
            assertThat(toma.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA);
            assertThat(toma.getId()).isEqualTo(SCHEDULE_ID);
        }
    }

    @Nested
    @DisplayName("companion VOs")
    class Refs {

        static Stream<Arguments> refsInvalidas() {
            return Stream.of(
                    arguments("EmployeeRef sin id",
                            (ThrowingCallable) () -> new EmployeeRef(null, "EMP-001", "Ana"),
                            "employee id is required"),
                    arguments("EmployeeRef sin codigo",
                            (ThrowingCallable) () -> new EmployeeRef(7L, "  ", "Ana"),
                            "employee code is required"),
                    arguments("EmployeeRef sin nombre",
                            (ThrowingCallable) () -> new EmployeeRef(7L, "EMP-001", null),
                            "employee name is required"),
                    arguments("HospitalizationProcedureRef sin id",
                            (ThrowingCallable) () -> new HospitalizationProcedureRef(null,
                                    "Curacion"),
                            "hospitalization procedure id is required"),
                    arguments("HospitalizationProcedureRef sin nombre",
                            (ThrowingCallable) () -> new HospitalizationProcedureRef(300L, ""),
                            "hospitalization procedure name is required"),
                    arguments("ProcedureOrderParams sin id",
                            (ThrowingCallable) () -> new ProcedureOrderParams(null, "Curacion",
                                    400L, "EVERY_8H", "FIXED", "DAYS", 3,
                                    ProcedureScheduleMother.INICIO,
                                    ProcedureScheduleMother.HORA_INICIO),
                            "procedure id is required"),
                    arguments("ProcedureOrderParams sin nombre",
                            (ThrowingCallable) () -> new ProcedureOrderParams(300L, "  ", 400L,
                                    "EVERY_8H", "FIXED", "DAYS", 3, ProcedureScheduleMother.INICIO,
                                    ProcedureScheduleMother.HORA_INICIO),
                            "procedure name is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("refsInvalidas")
        @DisplayName("rechazan")
        void rechazan(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("el nombre de la orden viaja al calendario para poder leerlo sin joins")
        void el_nombre_de_la_orden_viaja_al_calendario() {
            assertThat(ORDEN.name()).isEqualTo("Curacion de herida");
            assertThat(tomaPendiente().getHospitalizationProcedure()).isEqualTo(ORDEN);
        }

        @Test
        @DisplayName("dos refs con los mismos campos son la misma")
        void dos_refs_con_los_mismos_campos_son_la_misma() {
            assertThat(new EmployeeRef(7L, "EMP-001", "Ana Ruiz"))
                    .isEqualTo(new EmployeeRef(7L, "EMP-001", "Ana Ruiz"))
                    .hasSameHashCodeAs(new EmployeeRef(7L, "EMP-001", "Ana Ruiz"));
        }
    }
}
