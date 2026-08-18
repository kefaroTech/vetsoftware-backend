package com.vetsoftware.app.diagnosticimaging.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("DiagnosticImaging — invariantes y ciclo de vida")
class DiagnosticImagingTest {

    private static Builder valida() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = DiagnosticImagingMother.IMAGING_ID;
        private LocalDate date = DiagnosticImagingMother.FECHA;
        private DiagnosticImagingTypeRef type = DiagnosticImagingMother.TIPO;
        private String clinicalSigns = "Cojera pata trasera";
        private String studyType = "Radiografia de cadera";
        private String diagnosis = "Displasia leve";
        private String observations = "Control en 30 dias";
        private DiagnosticImagingStatus status = DiagnosticImagingStatus.PENDIENTE;
        private AnimalRef animal = DiagnosticImagingMother.MASCOTA;
        private ConsultationRef consultation = DiagnosticImagingMother.CONSULTA;
        private CompanyRef company = DiagnosticImagingMother.EMPRESA;

        private Builder date(LocalDate v) {
            this.date = v;
            return this;
        }

        private Builder type(DiagnosticImagingTypeRef v) {
            this.type = v;
            return this;
        }

        private Builder clinicalSigns(String v) {
            this.clinicalSigns = v;
            return this;
        }

        private Builder studyType(String v) {
            this.studyType = v;
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

        private Builder status(DiagnosticImagingStatus v) {
            this.status = v;
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

        private DiagnosticImaging build() {
            return new DiagnosticImaging(id, date, type, clinicalSigns, studyType, diagnosis,
                    observations, status, animal, consultation, company,
                    DiagnosticImagingMother.CREADO, true);
        }

        private void applyTo(DiagnosticImaging imaging) {
            imaging.update(date, type, clinicalSigns, studyType, diagnosis, observations, animal,
                    consultation, company);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            DiagnosticImaging imaging = valida().build();

            assertThat(imaging.getId()).isEqualTo(DiagnosticImagingMother.IMAGING_ID);
            assertThat(imaging.getDate()).isEqualTo(DiagnosticImagingMother.FECHA);
            assertThat(imaging.getDiagnosticImagingType()).isEqualTo(DiagnosticImagingMother.TIPO);
            assertThat(imaging.getClinicalSigns()).isEqualTo("Cojera pata trasera");
            assertThat(imaging.getStudyType()).isEqualTo("Radiografia de cadera");
            assertThat(imaging.getDiagnosis()).isEqualTo("Displasia leve");
            assertThat(imaging.getObservations()).isEqualTo("Control en 30 dias");
            assertThat(imaging.getStatus()).isEqualTo(DiagnosticImagingStatus.PENDIENTE);
            assertThat(imaging.getAnimal()).isEqualTo(DiagnosticImagingMother.MASCOTA);
            assertThat(imaging.getConsultation()).isEqualTo(DiagnosticImagingMother.CONSULTA);
            assertThat(imaging.getCompany()).isEqualTo(DiagnosticImagingMother.EMPRESA);
            assertThat(imaging.getCreatedDate()).isEqualTo(DiagnosticImagingMother.CREADO);
            assertThat(imaging.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, en PENDIENTE, con fecha de creacion y habilitada")
        void create_nace_sin_id_pendiente_y_habilitada() {
            DiagnosticImaging imaging = DiagnosticImagingMother.valida();

            assertThat(imaging.getId()).isNull();
            assertThat(imaging.getStatus()).isEqualTo(DiagnosticImagingStatus.PENDIENTE);
            assertThat(imaging.getCreatedDate()).isNotNull();
            assertThat(imaging.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la consulta es opcional")
        void la_consulta_es_opcional() {
            assertThatCode(() -> valida().consultation(null).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("las observaciones son opcionales")
        void las_observaciones_son_opcionales() {
            assertThatCode(() -> valida().observations(null).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("date null", (ThrowingCallable) () -> valida().date(null).build(),
                            "date is required"),
                    arguments("type null", (ThrowingCallable) () -> valida().type(null).build(),
                            "diagnosticImagingType is required"),
                    arguments("clinicalSigns null",
                            (ThrowingCallable) () -> valida().clinicalSigns(null).build(),
                            "clinicalSigns is required"),
                    arguments("clinicalSigns en blanco",
                            (ThrowingCallable) () -> valida().clinicalSigns("   ").build(),
                            "clinicalSigns is required"),
                    arguments("clinicalSigns de 2001 chars",
                            (ThrowingCallable) () -> valida().clinicalSigns("x".repeat(2001))
                                    .build(),
                            "clinicalSigns must be 2000 chars or less"),
                    arguments("studyType null",
                            (ThrowingCallable) () -> valida().studyType(null).build(),
                            "studyType is required"),
                    arguments("studyType en blanco",
                            (ThrowingCallable) () -> valida().studyType("   ").build(),
                            "studyType is required"),
                    arguments("studyType de 201 chars",
                            (ThrowingCallable) () -> valida().studyType("x".repeat(201)).build(),
                            "studyType must be 200 chars or less"),
                    arguments("diagnosis null",
                            (ThrowingCallable) () -> valida().diagnosis(null).build(),
                            "diagnosis is required"),
                    arguments("diagnosis en blanco",
                            (ThrowingCallable) () -> valida().diagnosis("   ").build(),
                            "diagnosis is required"),
                    arguments("diagnosis de 2001 chars",
                            (ThrowingCallable) () -> valida().diagnosis("x".repeat(2001)).build(),
                            "diagnosis must be 2000 chars or less"),
                    arguments("observations de 2001 chars",
                            (ThrowingCallable) () -> valida().observations("x".repeat(2001))
                                    .build(),
                            "observations must be 2000 chars or less"),
                    arguments("status null", (ThrowingCallable) () -> valida().status(null).build(),
                            "status is required"),
                    arguments("animal null", (ThrowingCallable) () -> valida().animal(null).build(),
                            "animal is required"),
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
        @DisplayName("clinicalSigns de exactamente 2000 chars se acepta")
        void clinical_signs_en_el_limite_se_acepta() {
            assertThatCode(() -> valida().clinicalSigns("x".repeat(2000)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("studyType de exactamente 200 chars se acepta")
        void study_type_en_el_limite_se_acepta() {
            assertThatCode(() -> valida().studyType("x".repeat(200)).build())
                    .doesNotThrowAnyException();
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
        @DisplayName("reemplaza los campos editables y conserva id, createdDate y status")
        void reemplaza_los_campos_editables() {
            DiagnosticImaging imaging = valida().build();
            AnimalRef otroAnimal = new AnimalRef(999L, "Michi", "A-002");

            valida().date(LocalDate.of(2026, 2, 1)).diagnosis("Displasia moderada")
                    .animal(otroAnimal).applyTo(imaging);

            assertThat(imaging.getDate()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(imaging.getDiagnosis()).isEqualTo("Displasia moderada");
            assertThat(imaging.getAnimal()).isEqualTo(otroAnimal);
            assertThat(imaging.getId()).isEqualTo(DiagnosticImagingMother.IMAGING_ID);
            assertThat(imaging.getCreatedDate()).isEqualTo(DiagnosticImagingMother.CREADO);
            assertThat(imaging.getStatus()).isEqualTo(DiagnosticImagingStatus.PENDIENTE);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            DiagnosticImaging imaging = valida().build();

            assertThatThrownBy(
                    () -> valida().diagnosis("Displasia moderada").animal(null).applyTo(imaging))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(imaging.getDiagnosis()).isEqualTo("Displasia leve");
            assertThat(imaging.getAnimal()).isEqualTo(DiagnosticImagingMother.MASCOTA);
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class CambioDeEstado {

        @ParameterizedTest
        @EnumSource(DiagnosticImagingStatus.class)
        @DisplayName("acepta cualquier estado del enum")
        void acepta_cualquier_estado_del_enum(DiagnosticImagingStatus nuevoEstado) {
            DiagnosticImaging imaging = valida().build();

            imaging.changeStatus(nuevoEstado);

            assertThat(imaging.getStatus()).isEqualTo(nuevoEstado);
        }

        @Test
        @DisplayName("un estado nulo no es valido")
        void un_estado_nulo_no_es_valido() {
            DiagnosticImaging imaging = valida().build();

            assertThatThrownBy(() -> imaging.changeStatus(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status is required");
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            DiagnosticImaging imaging = valida().build();

            imaging.disable();
            assertThat(imaging.isEnabled()).isFalse();
            imaging.disable();
            assertThat(imaging.isEnabled()).isFalse();

            imaging.enable();
            assertThat(imaging.isEnabled()).isTrue();
            imaging.enable();
            assertThat(imaging.isEnabled()).isTrue();
        }
    }
}
