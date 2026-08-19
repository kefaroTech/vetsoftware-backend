package com.vetsoftware.app.prescription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Prescription — invariantes y ciclo de vida")
class PrescriptionTest {

    private static Builder valida() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = PrescriptionMother.PRESCRIPTION_ID;
        private LocalDate date = PrescriptionMother.FECHA;
        private String diagnosis = "Otitis externa";
        private String observations = "Control en 7 dias";
        private AnimalRef animal = PrescriptionMother.MASCOTA;
        private ConsultationRef consultation = PrescriptionMother.CONSULTA;
        private CompanyRef company = PrescriptionMother.CLINICA;

        private Builder date(LocalDate v) {
            this.date = v;
            return this;
        }

        private Builder diagnosis(String v) {
            this.diagnosis = v;
            return this;
        }

        private Builder observations(String v) {
            this.observations = v;
            return this;
        }

        private Builder animal(AnimalRef v) {
            this.animal = v;
            return this;
        }

        private Builder consultation(ConsultationRef v) {
            this.consultation = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Prescription build() {
            return new Prescription(id, date, diagnosis, observations, animal, consultation,
                    company, PrescriptionMother.CREADO, null, true);
        }

        private void applyTo(Prescription prescription) {
            prescription.update(date, diagnosis, observations, animal, consultation, company);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Prescription prescription = valida().build();

            assertThat(prescription.getId()).isEqualTo(PrescriptionMother.PRESCRIPTION_ID);
            assertThat(prescription.getDate()).isEqualTo(PrescriptionMother.FECHA);
            assertThat(prescription.getDiagnosis()).isEqualTo("Otitis externa");
            assertThat(prescription.getObservations()).isEqualTo("Control en 7 dias");
            assertThat(prescription.getAnimal()).isEqualTo(PrescriptionMother.MASCOTA);
            assertThat(prescription.getConsultation()).isEqualTo(PrescriptionMother.CONSULTA);
            assertThat(prescription.getCompany()).isEqualTo(PrescriptionMother.CLINICA);
            assertThat(prescription.getCreatedDate()).isEqualTo(PrescriptionMother.CREADO);
            assertThat(prescription.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id y habilitada")
        void create_nace_sin_id_y_habilitada() {
            Prescription prescription = PrescriptionMother.valida();

            assertThat(prescription.getId()).isNull();
            assertThat(prescription.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("diagnostico y observaciones son opcionales y en blanco quedan como null")
        void diagnostico_y_observaciones_opcionales() {
            Prescription prescription = valida().diagnosis(null).observations(null).build();

            assertThat(prescription.getDiagnosis()).isNull();
            assertThat(prescription.getObservations()).isNull();
        }

        @Test
        @DisplayName("un diagnostico en blanco se normaliza a null")
        void diagnostico_en_blanco_se_normaliza_a_null() {
            Prescription prescription = valida().diagnosis("   ").build();

            assertThat(prescription.getDiagnosis()).isNull();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("date null", (ThrowingCallable) () -> valida().date(null).build(),
                            "date is required"),
                    arguments("diagnosis de 2001 chars",
                            (ThrowingCallable) () -> valida().diagnosis("x".repeat(2001)).build(),
                            "diagnosis must be 2000 chars or less"),
                    arguments("observations de 2001 chars",
                            (ThrowingCallable) () -> valida().observations("x".repeat(2001))
                                    .build(),
                            "observations must be 2000 chars or less"),
                    arguments("animal null", (ThrowingCallable) () -> valida().animal(null).build(),
                            "animal is required"),
                    arguments("consultation null",
                            (ThrowingCallable) () -> valida().consultation(null).build(),
                            "consultation is required"),
                    arguments("company null",
                            (ThrowingCallable) () -> valida().company(null).build(),
                            "company is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("diagnosis de exactamente 2000 chars se acepta")
        void diagnosis_en_el_limite_se_acepta() {
            assertThatCode(() -> valida().diagnosis("x".repeat(2000)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("observations de exactamente 2000 chars se acepta")
        void observations_en_el_limite_se_acepta() {
            assertThatCode(() -> valida().observations("x".repeat(2000)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            Prescription prescription = valida().build();
            AnimalRef otroAnimal = new AnimalRef(999L, "Michi", "A-002");

            valida().date(LocalDate.of(2026, 2, 1)).diagnosis("Dermatitis")
                    .observations("Revisar en 15 dias").animal(otroAnimal).applyTo(prescription);

            assertThat(prescription.getDate()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(prescription.getDiagnosis()).isEqualTo("Dermatitis");
            assertThat(prescription.getObservations()).isEqualTo("Revisar en 15 dias");
            assertThat(prescription.getAnimal()).isEqualTo(otroAnimal);
            assertThat(prescription.getId()).isEqualTo(PrescriptionMother.PRESCRIPTION_ID);
            assertThat(prescription.getCreatedDate()).isEqualTo(PrescriptionMother.CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            Prescription prescription = valida().build();

            assertThatThrownBy(
                    () -> valida().diagnosis("Dermatitis").animal(null).applyTo(prescription))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(prescription.getDiagnosis()).isEqualTo("Otitis externa");
            assertThat(prescription.getAnimal()).isEqualTo(PrescriptionMother.MASCOTA);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            Prescription prescription = valida().build();
            prescription.disable();

            valida().diagnosis("Dermatitis").applyTo(prescription);

            assertThat(prescription.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Prescription prescription = valida().build();

            prescription.disable();
            assertThat(prescription.isEnabled()).isFalse();
            prescription.disable();
            assertThat(prescription.isEnabled()).isFalse();

            prescription.enable();
            assertThat(prescription.isEnabled()).isTrue();
            prescription.enable();
            assertThat(prescription.isEnabled()).isTrue();
        }
    }
}
