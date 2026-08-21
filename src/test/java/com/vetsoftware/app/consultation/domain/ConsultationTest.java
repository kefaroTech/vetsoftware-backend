package com.vetsoftware.app.consultation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.consultation.testsupport.ConsultationMother;
import java.time.LocalDate;
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
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Consultation — invariantes y ciclo de vida del agregado")
class ConsultationTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir doce
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = ConsultationMother.CONSULTATION_ID;
        private LocalDate date = ConsultationMother.FECHA;
        private ConsultationTypeRef consultationType = ConsultationMother.CONTROL;
        private String anamnesis = "Anamnesis del paciente";
        private String diagnosis = "Diagnostico";
        private String prognosis = "Pronostico reservado";
        private PhysicalExam physicalExam = ConsultationMother.examenCompleto();
        private LocalDate nextControl = ConsultationMother.FECHA.plusDays(15);
        private AnimalRef animal = ConsultationMother.FIRULAIS;
        private CompanyRef company = ConsultationMother.CLINICA;

        private Builder anamnesis(String v) {
            this.anamnesis = v;
            return this;
        }

        private Builder diagnosis(String v) {
            this.diagnosis = v;
            return this;
        }

        private Builder prognosis(String v) {
            this.prognosis = v;
            return this;
        }

        private Builder date(LocalDate v) {
            this.date = v;
            return this;
        }

        private Builder consultationType(ConsultationTypeRef v) {
            this.consultationType = v;
            return this;
        }

        private Builder physicalExam(PhysicalExam v) {
            this.physicalExam = v;
            return this;
        }

        private Builder animal(AnimalRef v) {
            this.animal = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Consultation build() {
            return new Consultation(id, date, consultationType, anamnesis, diagnosis, prognosis,
                    physicalExam, nextControl, animal, company, ConsultationMother.CREADO, null,
                    true);
        }

    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Consultation consultation = valido().build();

            assertThat(consultation.getId()).isEqualTo(ConsultationMother.CONSULTATION_ID);
            assertThat(consultation.getDate()).isEqualTo(ConsultationMother.FECHA);
            assertThat(consultation.getConsultationType()).isEqualTo(ConsultationMother.CONTROL);
            assertThat(consultation.getAnamnesis()).isEqualTo("Anamnesis del paciente");
            assertThat(consultation.getDiagnosis()).isEqualTo("Diagnostico");
            assertThat(consultation.getPrognosis()).isEqualTo("Pronostico reservado");
            assertThat(consultation.getPhysicalExam())
                    .isEqualTo(ConsultationMother.examenCompleto());
            assertThat(consultation.getNextControl())
                    .isEqualTo(ConsultationMother.FECHA.plusDays(15));
            assertThat(consultation.getAnimal()).isEqualTo(ConsultationMother.FIRULAIS);
            assertThat(consultation.getCompany()).isEqualTo(ConsultationMother.CLINICA);
            assertThat(consultation.getCreatedDate()).isEqualTo(ConsultationMother.CREADO);
            assertThat(consultation.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitada y con createdDate propio")
        void create_nace_sin_id_y_habilitada() {
            Consultation consultation = Consultation.create(ConsultationMother.FECHA,
                    ConsultationMother.CONTROL, "Anamnesis", null, null, null,
                    ConsultationMother.FECHA.plusDays(15), ConsultationMother.FIRULAIS,
                    ConsultationMother.CLINICA);

            assertThat(consultation.getId()).isNull();
            assertThat(consultation.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(consultation.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("physicalExam null se normaliza a un examen vacio, nunca queda null")
        void physical_exam_null_se_normaliza_a_vacio() {
            Consultation consultation = valido().physicalExam(null).build();

            assertThat(consultation.getPhysicalExam()).isEqualTo(PhysicalExam.empty());
        }

        @Test
        @DisplayName("diagnosis y prognosis en blanco se normalizan a null")
        void diagnosis_y_prognosis_en_blanco_se_normalizan_a_null() {
            Consultation consultation = valido().diagnosis("   ").prognosis("").build();

            assertThat(consultation.getDiagnosis()).isNull();
            assertThat(consultation.getPrognosis()).isNull();
        }

        @Test
        @DisplayName("diagnosis y prognosis null son validos: son clinicamente opcionales")
        void diagnosis_y_prognosis_null_son_validos() {
            assertThatCode(() -> valido().diagnosis(null).prognosis(null).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("date null", (ThrowingCallable) () -> valido().date(null).build(),
                            "date is required"),
                    arguments("consultationType null",
                            (ThrowingCallable) () -> valido().consultationType(null).build(),
                            "consultationType is required"),
                    arguments("anamnesis null",
                            (ThrowingCallable) () -> valido().anamnesis(null).build(),
                            "anamnesis is required"),
                    arguments("anamnesis vacia",
                            (ThrowingCallable) () -> valido().anamnesis("").build(),
                            "anamnesis is required"),
                    arguments("anamnesis en blanco",
                            (ThrowingCallable) () -> valido().anamnesis("   ").build(),
                            "anamnesis is required"),
                    arguments("anamnesis de 2001 chars",
                            (ThrowingCallable) () -> valido().anamnesis("x".repeat(2001)).build(),
                            "anamnesis must be 2000 chars or less"),
                    arguments("diagnosis de 2001 chars",
                            (ThrowingCallable) () -> valido().diagnosis("x".repeat(2001)).build(),
                            "diagnosis must be 2000 chars or less"),
                    arguments("prognosis de 501 chars",
                            (ThrowingCallable) () -> valido().prognosis("x".repeat(501)).build(),
                            "prognosis must be 500 chars or less"),
                    arguments("animal null", (ThrowingCallable) () -> valido().animal(null).build(),
                            "animal is required"),
                    arguments("company null",
                            (ThrowingCallable) () -> valido().company(null).build(),
                            "company is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 2000})
        @DisplayName("anamnesis en el limite exacto se acepta")
        void anamnesis_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().anamnesis("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("diagnosis de exactamente 2000 chars se acepta")
        void diagnosis_de_2000_chars_se_acepta() {
            assertThatCode(() -> valido().diagnosis("x".repeat(2000)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("prognosis de exactamente 500 chars se acepta")
        void prognosis_de_500_chars_se_acepta() {
            assertThatCode(() -> valido().prognosis("x".repeat(500)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Consultation consultation = valido().build();

            consultation.disable();
            assertThat(consultation.isEnabled()).isFalse();
            consultation.disable();
            assertThat(consultation.isEnabled()).isFalse();

            consultation.enable();
            assertThat(consultation.isEnabled()).isTrue();
            consultation.enable();
            assertThat(consultation.isEnabled()).isTrue();
        }
    }
}
