package com.vetsoftware.app.hospitalizationprocedure.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.hospitalizationprocedure.testsupport.HospitalizationProcedureMother;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("HospitalizationProcedure — invariantes y ciclo de vida del agregado")
class HospitalizationProcedureTest {

    private static Builder valido() {
        return new Builder();
    }

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir
     * dieciseis argumentos en cada escenario invalido, que es como se cuela un test
     * que valida un campo distinto del que dice validar.
     */
    private static final class Builder {
        private Long id = HospitalizationProcedureMother.PROCEDURE_ID;
        private String name = "Curacion de herida";
        private String dose = "Solucion salina 0.9%";
        private Frequency frequency = Frequency.EVERY_8H;
        private GuidelineType guidelineType = GuidelineType.INTERVAL;
        private DurationMeasure durationMeasure = DurationMeasure.DAYS;
        private Integer durationQuantity = 5;
        private LocalDate startDate = LocalDate.of(2026, 3, 1);
        private LocalTime startTime = LocalTime.of(8, 0);
        private String notes = "Cambiar el vendaje cada turno";
        private HospitalizationRef hospitalization = HospitalizationProcedureMother.HOSPITALIZACION;
        private EmployeeRef createdBy = HospitalizationProcedureMother.CREADO_POR;
        private LocalDateTime createdDate = HospitalizationProcedureMother.CREADO;
        private boolean enabled = true;
        private LocalDateTime suspensionDate;
        private EmployeeRef suspensionBy;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder dose(String v) {
            this.dose = v;
            return this;
        }

        private Builder notes(String v) {
            this.notes = v;
            return this;
        }

        private Builder hospitalization(HospitalizationRef v) {
            this.hospitalization = v;
            return this;
        }

        private Builder createdBy(EmployeeRef v) {
            this.createdBy = v;
            return this;
        }

        private Builder frequency(Frequency v) {
            this.frequency = v;
            return this;
        }

        private HospitalizationProcedure build() {
            return new HospitalizationProcedure(id, name, dose, frequency, guidelineType,
                    durationMeasure, durationQuantity, startDate, startTime, notes, hospitalization,
                    createdBy, createdDate, enabled, suspensionDate, suspensionBy);
        }

        private void applyTo(HospitalizationProcedure procedure) {
            procedure.update(name, dose, frequency, guidelineType, durationMeasure,
                    durationQuantity, startDate, startTime, notes);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            HospitalizationProcedure procedure = valido().build();

            assertThat(procedure.getId()).isEqualTo(HospitalizationProcedureMother.PROCEDURE_ID);
            assertThat(procedure.getName()).isEqualTo("Curacion de herida");
            assertThat(procedure.getDose()).isEqualTo("Solucion salina 0.9%");
            assertThat(procedure.getFrequency()).isEqualTo(Frequency.EVERY_8H);
            assertThat(procedure.getGuidelineType()).isEqualTo(GuidelineType.INTERVAL);
            assertThat(procedure.getDurationMeasure()).isEqualTo(DurationMeasure.DAYS);
            assertThat(procedure.getDurationQuantity()).isEqualTo(5);
            assertThat(procedure.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(procedure.getStartTime()).isEqualTo(LocalTime.of(8, 0));
            assertThat(procedure.getNotes()).isEqualTo("Cambiar el vendaje cada turno");
            assertThat(procedure.getHospitalization())
                    .isEqualTo(HospitalizationProcedureMother.HOSPITALIZACION);
            assertThat(procedure.getCreatedBy())
                    .isEqualTo(HospitalizationProcedureMother.CREADO_POR);
            assertThat(procedure.getCreatedDate()).isEqualTo(HospitalizationProcedureMother.CREADO);
            assertThat(procedure.isEnabled()).isTrue();
            assertThat(procedure.getSuspensionDate()).isNull();
            assertThat(procedure.getSuspensionBy()).isNull();
        }

        @Test
        @DisplayName("create() nace sin id, habilitada y sin suspension")
        void create_nace_sin_id_habilitada_y_sin_suspension() {
            HospitalizationProcedure procedure = HospitalizationProcedure.create(
                    "Curacion de herida", "Solucion salina 0.9%", Frequency.EVERY_8H,
                    GuidelineType.INTERVAL, DurationMeasure.DAYS, 5, LocalDate.of(2026, 3, 1),
                    LocalTime.of(8, 0), "Cambiar el vendaje cada turno",
                    HospitalizationProcedureMother.HOSPITALIZACION,
                    HospitalizationProcedureMother.CREADO_POR);

            assertThat(procedure.getId()).isNull();
            assertThat(procedure.isEnabled()).isTrue();
            assertThat(procedure.getSuspensionDate()).isNull();
            assertThat(procedure.getSuspensionBy()).isNull();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion es una ventana. Deuda anotada en el
            // CLAUDE.md, igual que Animal.create.
            assertThat(procedure.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("dose y notes son opcionales: una orden sin dosis ni notas es valida")
        void dose_y_notes_son_opcionales() {
            assertThatCode(() -> valido().dose(null).notes(null).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null", (ThrowingCallable) () -> valido().name(null).build(),
                            "name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> valido().name("").build(),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> valido().name("   ").build(),
                            "name is required"),
                    arguments("name de 201 chars",
                            (ThrowingCallable) () -> valido().name("x".repeat(201)).build(),
                            "name must be 200 chars or less"),
                    arguments("dose de 201 chars",
                            (ThrowingCallable) () -> valido().dose("x".repeat(201)).build(),
                            "dose must be 200 chars or less"),
                    arguments("notes de 2001 chars",
                            (ThrowingCallable) () -> valido().notes("x".repeat(2001)).build(),
                            "notes must be 2000 chars or less"),
                    arguments("hospitalization null",
                            (ThrowingCallable) () -> valido().hospitalization(null).build(),
                            "hospitalization is required"),
                    arguments("createdBy null",
                            (ThrowingCallable) () -> valido().createdBy(null).build(),
                            "createdBy is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 200})
        @DisplayName("name en el limite exacto se acepta")
        void name_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().name("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("dose de 200 chars se acepta")
        void dose_de_200_chars_se_acepta() {
            assertThatCode(() -> valido().dose("x".repeat(200)).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("notes de 2000 chars se acepta")
        void notes_de_2000_chars_se_acepta() {
            assertThatCode(() -> valido().notes("x".repeat(2000)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id, hospitalizacion y createdBy")
        void reemplaza_los_campos_mutables_y_conserva_id_hospitalizacion_y_created_by() {
            HospitalizationProcedure procedure = valido().build();

            valido().name("Curacion actualizada").dose("Suero fisiologico")
                    .frequency(Frequency.EVERY_12H).notes("Notas nuevas").applyTo(procedure);

            assertThat(procedure.getName()).isEqualTo("Curacion actualizada");
            assertThat(procedure.getDose()).isEqualTo("Suero fisiologico");
            assertThat(procedure.getFrequency()).isEqualTo(Frequency.EVERY_12H);
            assertThat(procedure.getNotes()).isEqualTo("Notas nuevas");
            assertThat(procedure.getId()).isEqualTo(HospitalizationProcedureMother.PROCEDURE_ID);
            assertThat(procedure.getHospitalization())
                    .isEqualTo(HospitalizationProcedureMother.HOSPITALIZACION);
            assertThat(procedure.getCreatedBy())
                    .isEqualTo(HospitalizationProcedureMother.CREADO_POR);
            assertThat(procedure.getCreatedDate()).isEqualTo(HospitalizationProcedureMother.CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            HospitalizationProcedure procedure = valido().build();

            // El nombre nuevo es valido y la dosis no: si validate() no corriera ANTES
            // de asignar, la orden se quedaria con el nombre nuevo y la dosis vieja
            // fuera de rango.
            assertThatThrownBy(
                    () -> valido().name("Curacion nueva").dose("x".repeat(201)).applyTo(procedure))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(procedure.getName()).isEqualTo("Curacion de herida");
            assertThat(procedure.getDose()).isEqualTo("Solucion salina 0.9%");
        }

        @Test
        @DisplayName("no toca el estado de habilitacion ni de suspension")
        void no_toca_el_estado_de_habilitacion_ni_de_suspension() {
            HospitalizationProcedure procedure = valido().build();
            procedure.disable();
            procedure.suspend(HospitalizationProcedureMother.SUSPENDIDO_POR,
                    HospitalizationProcedureMother.SUSPENDIDO_EL);

            valido().name("Curacion actualizada").applyTo(procedure);

            assertThat(procedure.isEnabled()).isFalse();
            assertThat(procedure.getSuspensionBy())
                    .isEqualTo(HospitalizationProcedureMother.SUSPENDIDO_POR);
        }
    }

    @Nested
    @DisplayName("suspend")
    class Suspend {

        @Test
        @DisplayName("registra quien y cuando sin tocar las ejecuciones ya aplicadas")
        void registra_quien_y_cuando() {
            HospitalizationProcedure procedure = valido().build();

            procedure.suspend(HospitalizationProcedureMother.SUSPENDIDO_POR,
                    HospitalizationProcedureMother.SUSPENDIDO_EL);

            assertThat(procedure.getSuspensionBy())
                    .isEqualTo(HospitalizationProcedureMother.SUSPENDIDO_POR);
            assertThat(procedure.getSuspensionDate())
                    .isEqualTo(HospitalizationProcedureMother.SUSPENDIDO_EL);
        }

        @Test
        @DisplayName("rechaza un empleado nulo")
        void rechaza_un_empleado_nulo() {
            HospitalizationProcedure procedure = valido().build();

            assertThatThrownBy(
                    () -> procedure.suspend(null, HospitalizationProcedureMother.SUSPENDIDO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("suspensionBy is required");
        }

        @Test
        @DisplayName("rechaza una fecha nula")
        void rechaza_una_fecha_nula() {
            HospitalizationProcedure procedure = valido().build();

            assertThatThrownBy(
                    () -> procedure.suspend(HospitalizationProcedureMother.SUSPENDIDO_POR, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("suspensionDate is required");
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            HospitalizationProcedure procedure = valido().build();

            procedure.disable();
            assertThat(procedure.isEnabled()).isFalse();
            procedure.disable();
            assertThat(procedure.isEnabled()).isFalse();

            procedure.enable();
            assertThat(procedure.isEnabled()).isTrue();
            procedure.enable();
            assertThat(procedure.isEnabled()).isTrue();
        }
    }
}
