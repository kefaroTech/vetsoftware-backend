package com.vetsoftware.app.hospitalizationmedication.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.hospitalizationmedication.testsupport.HospitalizationMedicationMother;
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

@DisplayName("HospitalizationMedication — invariantes y ciclo de vida del agregado")
class HospitalizationMedicationTest {

    private static Builder valido() {
        return new Builder();
    }

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir
     * dieciseis argumentos en cada escenario invalido, que es como se cuela un test
     * que valida un campo distinto del que dice validar.
     */
    private static final class Builder {
        private Long id = HospitalizationMedicationMother.MEDICATION_ID;
        private String name = "Amoxicilina 500mg";
        private String dose = "1 tableta";
        private Frequency frequency = Frequency.EVERY_8H;
        private GuidelineType guidelineType = GuidelineType.INTERVAL;
        private DurationMeasure durationMeasure = DurationMeasure.DAYS;
        private Integer durationQuantity = 5;
        private LocalDate startDate = LocalDate.of(2026, 3, 1);
        private LocalTime startTime = LocalTime.of(8, 0);
        private String notes = "Administrar con alimento";
        private HospitalizationRef hospitalization = HospitalizationMedicationMother.HOSPITALIZACION;
        private EmployeeRef createdBy = HospitalizationMedicationMother.CREADO_POR;
        private LocalDateTime createdDate = HospitalizationMedicationMother.CREADO;
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

        private HospitalizationMedication build() {
            return new HospitalizationMedication(id, name, dose, frequency, guidelineType,
                    durationMeasure, durationQuantity, startDate, startTime, notes, hospitalization,
                    createdBy, createdDate, enabled, suspensionDate, suspensionBy);
        }

        private void applyTo(HospitalizationMedication medication) {
            medication.update(name, dose, frequency, guidelineType, durationMeasure,
                    durationQuantity, startDate, startTime, notes);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            HospitalizationMedication medication = valido().build();

            assertThat(medication.getId()).isEqualTo(HospitalizationMedicationMother.MEDICATION_ID);
            assertThat(medication.getName()).isEqualTo("Amoxicilina 500mg");
            assertThat(medication.getDose()).isEqualTo("1 tableta");
            assertThat(medication.getFrequency()).isEqualTo(Frequency.EVERY_8H);
            assertThat(medication.getGuidelineType()).isEqualTo(GuidelineType.INTERVAL);
            assertThat(medication.getDurationMeasure()).isEqualTo(DurationMeasure.DAYS);
            assertThat(medication.getDurationQuantity()).isEqualTo(5);
            assertThat(medication.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(medication.getStartTime()).isEqualTo(LocalTime.of(8, 0));
            assertThat(medication.getNotes()).isEqualTo("Administrar con alimento");
            assertThat(medication.getHospitalization())
                    .isEqualTo(HospitalizationMedicationMother.HOSPITALIZACION);
            assertThat(medication.getCreatedBy())
                    .isEqualTo(HospitalizationMedicationMother.CREADO_POR);
            assertThat(medication.getCreatedDate())
                    .isEqualTo(HospitalizationMedicationMother.CREADO);
            assertThat(medication.isEnabled()).isTrue();
            assertThat(medication.getSuspensionDate()).isNull();
            assertThat(medication.getSuspensionBy()).isNull();
        }

        @Test
        @DisplayName("create() nace sin id, habilitada y sin suspension")
        void create_nace_sin_id_habilitada_y_sin_suspension() {
            HospitalizationMedication medication = HospitalizationMedication.create(
                    "Amoxicilina 500mg", "1 tableta", Frequency.EVERY_8H, GuidelineType.INTERVAL,
                    DurationMeasure.DAYS, 5, LocalDate.of(2026, 3, 1), LocalTime.of(8, 0),
                    "Administrar con alimento", HospitalizationMedicationMother.HOSPITALIZACION,
                    HospitalizationMedicationMother.CREADO_POR);

            assertThat(medication.getId()).isNull();
            assertThat(medication.isEnabled()).isTrue();
            assertThat(medication.getSuspensionDate()).isNull();
            assertThat(medication.getSuspensionBy()).isNull();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion es una ventana. Deuda anotada en el
            // CLAUDE.md, igual que Animal.create.
            assertThat(medication.getCreatedDate()).isCloseTo(LocalDateTime.now(),
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
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id, hospitalizacion y createdBy")
        void reemplaza_los_campos_mutables_y_conserva_id_hospitalizacion_y_created_by() {
            HospitalizationMedication medication = valido().build();

            valido().name("Amoxicilina 1g").dose("2 tabletas").frequency(Frequency.EVERY_12H)
                    .notes("Notas nuevas").applyTo(medication);

            assertThat(medication.getName()).isEqualTo("Amoxicilina 1g");
            assertThat(medication.getDose()).isEqualTo("2 tabletas");
            assertThat(medication.getFrequency()).isEqualTo(Frequency.EVERY_12H);
            assertThat(medication.getNotes()).isEqualTo("Notas nuevas");
            assertThat(medication.getId()).isEqualTo(HospitalizationMedicationMother.MEDICATION_ID);
            assertThat(medication.getHospitalization())
                    .isEqualTo(HospitalizationMedicationMother.HOSPITALIZACION);
            assertThat(medication.getCreatedBy())
                    .isEqualTo(HospitalizationMedicationMother.CREADO_POR);
            assertThat(medication.getCreatedDate())
                    .isEqualTo(HospitalizationMedicationMother.CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            HospitalizationMedication medication = valido().build();

            // El nombre nuevo es invalido: si validate() no corriera ANTES de asignar,
            // la orden se quedaria con la dosis nueva y el nombre vacio.
            assertThatThrownBy(() -> valido().name("   ").dose("2 tabletas").applyTo(medication))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(medication.getName()).isEqualTo("Amoxicilina 500mg");
            assertThat(medication.getDose()).isEqualTo("1 tableta");
        }

        @Test
        @DisplayName("no toca el estado de habilitacion ni de suspension")
        void no_toca_el_estado_de_habilitacion_ni_de_suspension() {
            HospitalizationMedication medication = valido().build();
            medication.disable();
            medication.suspend(HospitalizationMedicationMother.SUSPENDIDO_POR,
                    HospitalizationMedicationMother.SUSPENDIDO_EL);

            valido().name("Amoxicilina 1g").applyTo(medication);

            assertThat(medication.isEnabled()).isFalse();
            assertThat(medication.getSuspensionBy())
                    .isEqualTo(HospitalizationMedicationMother.SUSPENDIDO_POR);
        }
    }

    @Nested
    @DisplayName("suspend")
    class Suspend {

        @Test
        @DisplayName("registra quien y cuando sin tocar las dosis ya aplicadas")
        void registra_quien_y_cuando() {
            HospitalizationMedication medication = valido().build();

            medication.suspend(HospitalizationMedicationMother.SUSPENDIDO_POR,
                    HospitalizationMedicationMother.SUSPENDIDO_EL);

            assertThat(medication.getSuspensionBy())
                    .isEqualTo(HospitalizationMedicationMother.SUSPENDIDO_POR);
            assertThat(medication.getSuspensionDate())
                    .isEqualTo(HospitalizationMedicationMother.SUSPENDIDO_EL);
        }

        @Test
        @DisplayName("rechaza un empleado nulo")
        void rechaza_un_empleado_nulo() {
            HospitalizationMedication medication = valido().build();

            assertThatThrownBy(
                    () -> medication.suspend(null, HospitalizationMedicationMother.SUSPENDIDO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("suspensionBy is required");
        }

        @Test
        @DisplayName("rechaza una fecha nula")
        void rechaza_una_fecha_nula() {
            HospitalizationMedication medication = valido().build();

            assertThatThrownBy(
                    () -> medication.suspend(HospitalizationMedicationMother.SUSPENDIDO_POR, null))
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
            HospitalizationMedication medication = valido().build();

            medication.disable();
            assertThat(medication.isEnabled()).isFalse();
            medication.disable();
            assertThat(medication.isEnabled()).isFalse();

            medication.enable();
            assertThat(medication.isEnabled()).isTrue();
            medication.enable();
            assertThat(medication.isEnabled()).isTrue();
        }
    }
}
