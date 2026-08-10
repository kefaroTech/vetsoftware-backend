package com.vetsoftware.app.hospitalization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.hospitalization.testsupport.HospitalizationMother;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Hospitalization — invariantes y ciclo de vida del agregado")
class HospitalizationTest {

    private static Builder valido() {
        return new Builder();
    }

    /**
     * Constructor de fixtures con un campo variable por caso: evita repetir trece
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static final class Builder {
        private Long id = HospitalizationMother.HOSPITALIZATION_ID;
        private LocalDate date = HospitalizationMother.FECHA;
        private LocalDate startDate = HospitalizationMother.INICIO;
        private LocalDate endDate = HospitalizationMother.FIN;
        private HospitalizationType type = HospitalizationType.HOSPITALIZATION;
        private ReasonLeaving reasonLeaving = ReasonLeaving.MEDICAL_DISCHARGE;
        private String reason = "Gastroenteritis aguda";
        private String observations = "Sin complicaciones";
        private AnimalRef animal = HospitalizationMother.FIRULAIS;
        private ConsultationRef consultation = HospitalizationMother.CONSULTA;
        private CompanyRef company = HospitalizationMother.CLINICA;

        private Builder date(LocalDate v) {
            this.date = v;
            return this;
        }

        private Builder startDate(LocalDate v) {
            this.startDate = v;
            return this;
        }

        private Builder endDate(LocalDate v) {
            this.endDate = v;
            return this;
        }

        private Builder type(HospitalizationType v) {
            this.type = v;
            return this;
        }

        private Builder reasonLeaving(ReasonLeaving v) {
            this.reasonLeaving = v;
            return this;
        }

        private Builder reason(String v) {
            this.reason = v;
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

        private Hospitalization build() {
            return new Hospitalization(id, date, startDate, endDate, type, reasonLeaving, reason,
                    observations, animal, consultation, company, HospitalizationMother.CREADO,
                    true);
        }

        private void applyTo(Hospitalization hospitalization) {
            hospitalization.update(date, startDate, endDate, type, reasonLeaving, reason,
                    observations, animal, consultation, company);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Hospitalization hospitalization = valido().build();

            assertThat(hospitalization.getId()).isEqualTo(HospitalizationMother.HOSPITALIZATION_ID);
            assertThat(hospitalization.getDate()).isEqualTo(HospitalizationMother.FECHA);
            assertThat(hospitalization.getStartDate()).isEqualTo(HospitalizationMother.INICIO);
            assertThat(hospitalization.getEndDate()).isEqualTo(HospitalizationMother.FIN);
            assertThat(hospitalization.getType()).isEqualTo(HospitalizationType.HOSPITALIZATION);
            assertThat(hospitalization.getReasonLeaving())
                    .isEqualTo(ReasonLeaving.MEDICAL_DISCHARGE);
            assertThat(hospitalization.getReason()).isEqualTo("Gastroenteritis aguda");
            assertThat(hospitalization.getObservations()).isEqualTo("Sin complicaciones");
            assertThat(hospitalization.getAnimal()).isEqualTo(HospitalizationMother.FIRULAIS);
            assertThat(hospitalization.getConsultation()).isEqualTo(HospitalizationMother.CONSULTA);
            assertThat(hospitalization.getCompany()).isEqualTo(HospitalizationMother.CLINICA);
            assertThat(hospitalization.getCreatedDate()).isEqualTo(HospitalizationMother.CREADO);
            assertThat(hospitalization.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con la fecha de creacion de ahora")
        void create_nace_sin_id_y_habilitado() {
            Hospitalization hospitalization = Hospitalization.create(HospitalizationMother.FECHA,
                    HospitalizationMother.INICIO, HospitalizationMother.FIN,
                    HospitalizationType.HOSPITALIZATION, ReasonLeaving.MEDICAL_DISCHARGE,
                    "Gastroenteritis aguda", "Sin complicaciones", HospitalizationMother.FIRULAIS,
                    HospitalizationMother.CONSULTA, HospitalizationMother.CLINICA);

            assertThat(hospitalization.getId()).isNull();
            assertThat(hospitalization.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana.
            assertThat(hospitalization.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("endDate es opcional: una hospitalizacion en curso es valida")
        void end_date_es_opcional() {
            assertThatCode(() -> valido().endDate(null).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("reasonLeaving, observations y consultation son opcionales")
        void campos_opcionales() {
            Hospitalization hospitalization = valido().reasonLeaving(null).observations(null)
                    .consultation(null).build();

            assertThat(hospitalization.getReasonLeaving()).isNull();
            assertThat(hospitalization.getObservations()).isNull();
            assertThat(hospitalization.getConsultation()).isNull();
        }

        @Test
        @DisplayName("endDate igual a startDate es valido: ingreso y alta el mismo dia")
        void end_date_igual_a_start_date_es_valido() {
            assertThatCode(() -> valido().startDate(LocalDate.of(2026, 3, 1))
                    .endDate(LocalDate.of(2026, 3, 1)).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("date nula", (Runnable) () -> valido().date(null).build(),
                            "date is required"),
                    arguments("startDate nula", (Runnable) () -> valido().startDate(null).build(),
                            "startDate is required"),
                    arguments("endDate anterior a startDate",
                            (Runnable) () -> valido().startDate(LocalDate.of(2026, 3, 5))
                                    .endDate(LocalDate.of(2026, 3, 4)).build(),
                            "endDate cannot be before startDate"),
                    arguments("type nulo", (Runnable) () -> valido().type(null).build(),
                            "type is required"),
                    arguments("reason nulo", (Runnable) () -> valido().reason(null).build(),
                            "reason is required"),
                    arguments("reason en blanco", (Runnable) () -> valido().reason("   ").build(),
                            "reason is required"),
                    arguments("reason de 501 caracteres",
                            (Runnable) () -> valido().reason("x".repeat(501)).build(),
                            "reason must be 500 chars or less"),
                    arguments("observations de 2001 caracteres",
                            (Runnable) () -> valido().observations("x".repeat(2001)).build(),
                            "observations must be 2000 chars or less"),
                    arguments("animal nulo", (Runnable) () -> valido().animal(null).build(),
                            "animal is required"),
                    arguments("company nula", (Runnable) () -> valido().company(null).build(),
                            "company is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza cada invariante rota")
        void el_constructor_rechaza_cada_invariante_rota(String caso, Runnable construccion,
                String mensaje) {
            assertThatThrownBy(construccion::run).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("reason de exactamente 500 caracteres pasa el limite")
        void reason_de_500_caracteres_pasa() {
            Hospitalization hospitalization = valido().reason("x".repeat(500)).build();

            assertThat(hospitalization.getReason()).hasSize(500);
        }

        @Test
        @DisplayName("observations de exactamente 2000 caracteres pasa el limite")
        void observations_de_2000_caracteres_pasa() {
            Hospitalization hospitalization = valido().observations("x".repeat(2000)).build();

            assertThat(hospitalization.getObservations()).hasSize(2000);
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("update() reemplaza los campos mutables y respeta id y createdDate")
        void update_reemplaza_los_campos_mutables() {
            Hospitalization hospitalization = HospitalizationMother.internado();
            AnimalRef otroAnimal = new AnimalRef(99L, "Michi", "A-099");
            CompanyRef otraCompany = new CompanyRef(77L, "Otra Clinica", "800999888");

            hospitalization.update(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 4, 3), HospitalizationType.OUTPATIENT, ReasonLeaving.DEATH,
                    "Nuevo motivo", "Nuevas observaciones", otroAnimal, null, otraCompany);

            assertThat(hospitalization.getDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(hospitalization.getStartDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(hospitalization.getEndDate()).isEqualTo(LocalDate.of(2026, 4, 3));
            assertThat(hospitalization.getType()).isEqualTo(HospitalizationType.OUTPATIENT);
            assertThat(hospitalization.getReasonLeaving()).isEqualTo(ReasonLeaving.DEATH);
            assertThat(hospitalization.getReason()).isEqualTo("Nuevo motivo");
            assertThat(hospitalization.getObservations()).isEqualTo("Nuevas observaciones");
            assertThat(hospitalization.getAnimal()).isEqualTo(otroAnimal);
            assertThat(hospitalization.getConsultation()).isNull();
            assertThat(hospitalization.getCompany()).isEqualTo(otraCompany);
            assertThat(hospitalization.getId()).isEqualTo(HospitalizationMother.HOSPITALIZATION_ID);
            assertThat(hospitalization.getCreatedDate()).isEqualTo(HospitalizationMother.CREADO);
        }

        @Test
        @DisplayName("update() aplica las mismas invariantes que el constructor")
        void update_aplica_las_mismas_invariantes() {
            Hospitalization hospitalization = HospitalizationMother.internado();

            assertThatThrownBy(() -> valido().reason("  ").applyTo(hospitalization))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required");
        }

        @Test
        @DisplayName("update() rechazado no deja el agregado a medio escribir")
        void update_rechazado_no_deja_el_agregado_a_medio_escribir() {
            Hospitalization hospitalization = HospitalizationMother.internado();

            ThrowingCallable actualizacionInvalida = () -> valido().animal(null)
                    .reason("Motivo nuevo").applyTo(hospitalization);

            assertThatThrownBy(actualizacionInvalida).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal is required");
            assertThat(hospitalization.getReason()).isEqualTo("Gastroenteritis aguda");
            assertThat(hospitalization.getAnimal()).isEqualTo(HospitalizationMother.FIRULAIS);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable() marca la hospitalizacion como deshabilitada")
        void disable_marca_la_hospitalizacion_como_deshabilitada() {
            Hospitalization hospitalization = HospitalizationMother.internado();

            hospitalization.disable();

            assertThat(hospitalization.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable() la vuelve a habilitar")
        void enable_la_vuelve_a_habilitar() {
            Hospitalization hospitalization = HospitalizationMother.deshabilitado();

            hospitalization.enable();

            assertThat(hospitalization.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("enums del agregado")
    class Enums {

        @ParameterizedTest
        @EnumSource(HospitalizationType.class)
        @DisplayName("cualquier tipo de hospitalizacion es aceptado por el agregado")
        void cualquier_tipo_es_aceptado(HospitalizationType tipo) {
            assertThat(valido().type(tipo).build().getType()).isEqualTo(tipo);
        }

        @ParameterizedTest
        @EnumSource(ReasonLeaving.class)
        @DisplayName("cualquier motivo de salida es aceptado por el agregado")
        void cualquier_motivo_de_salida_es_aceptado(ReasonLeaving motivo) {
            assertThat(valido().reasonLeaving(motivo).build().getReasonLeaving()).isEqualTo(motivo);
        }

        @Test
        @DisplayName("HospitalizationType tiene exactamente las dos modalidades del negocio")
        void hospitalization_type_tiene_dos_modalidades() {
            assertThat(HospitalizationType.values()).containsExactly(HospitalizationType.OUTPATIENT,
                    HospitalizationType.HOSPITALIZATION);
        }

        @Test
        @DisplayName("ReasonLeaving cubre los siete motivos de alta del negocio")
        void reason_leaving_cubre_los_siete_motivos() {
            assertThat(ReasonLeaving.values()).containsExactly(ReasonLeaving.MEDICAL_DISCHARGE,
                    ReasonLeaving.HOME_TREATMENT, ReasonLeaving.TRANSFER, ReasonLeaving.TUTOR_WISH,
                    ReasonLeaving.ADMIN, ReasonLeaving.DEATH, ReasonLeaving.EUTHANASIA);
        }
    }

    @Nested
    @DisplayName("excepcion de dominio")
    class Excepcion {

        @Test
        @DisplayName("HospitalizationNotFoundException lleva el id en el mensaje")
        void not_found_lleva_el_id_en_el_mensaje() {
            assertThat(new HospitalizationNotFoundException(42L))
                    .hasMessage("Hospitalization not found: 42")
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
