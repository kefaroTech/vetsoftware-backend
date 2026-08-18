package com.vetsoftware.app.deworming.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.deworming.testsupport.DewormingMother;
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

@DisplayName("Deworming — invariantes y ciclo de vida del agregado")
class DewormingTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir trece
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 1L;
        private LocalDate date = LocalDate.of(2026, 4, 1);
        private LocalDate lastDeworming = LocalDate.of(2026, 1, 1);
        private DewormingType type = DewormingType.INTERNAL;
        private String product = "Drontal Plus";
        private String dosage = "1 tableta / 10kg";
        private LocalDate nextControl = LocalDate.of(2026, 7, 1);
        private String observations = "Sin reacciones adversas";
        private AnimalRef animal = DewormingMother.FIRULAIS;
        private ConsultationRef consultation = DewormingMother.CONSULTA;
        private CompanyRef company = DewormingMother.CLINICA;

        private Builder date(LocalDate v) {
            this.date = v;
            return this;
        }

        private Builder type(DewormingType v) {
            this.type = v;
            return this;
        }

        private Builder product(String v) {
            this.product = v;
            return this;
        }

        private Builder dosage(String v) {
            this.dosage = v;
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

        private Deworming build() {
            return new Deworming(id, date, lastDeworming, type, product, dosage, nextControl,
                    observations, animal, consultation, company, DewormingMother.CREADO, true);
        }

        private void applyTo(Deworming deworming) {
            deworming.update(date, lastDeworming, type, product, dosage, nextControl, observations,
                    animal, consultation, company);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Deworming deworming = valido().build();

            assertThat(deworming.getId()).isEqualTo(1L);
            assertThat(deworming.getDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(deworming.getLastDeworming()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(deworming.getType()).isEqualTo(DewormingType.INTERNAL);
            assertThat(deworming.getProduct()).isEqualTo("Drontal Plus");
            assertThat(deworming.getDosage()).isEqualTo("1 tableta / 10kg");
            assertThat(deworming.getNextControl()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(deworming.getObservations()).isEqualTo("Sin reacciones adversas");
            assertThat(deworming.getAnimal()).isEqualTo(DewormingMother.FIRULAIS);
            assertThat(deworming.getConsultation()).isEqualTo(DewormingMother.CONSULTA);
            assertThat(deworming.getCompany()).isEqualTo(DewormingMother.CLINICA);
            assertThat(deworming.getCreatedDate()).isEqualTo(DewormingMother.CREADO);
            assertThat(deworming.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitada y con createdDate ahora")
        void create_nace_sin_id_habilitada() {
            Deworming deworming = Deworming.create(LocalDate.of(2026, 4, 1), null,
                    DewormingType.INTERNAL, "Drontal Plus", "1 tableta / 10kg", null, null,
                    DewormingMother.FIRULAIS, null, DewormingMother.CLINICA);

            assertThat(deworming.getId()).isNull();
            assertThat(deworming.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(deworming.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("lastDeworming, nextControl, observations y consultation son opcionales")
        void campos_opcionales_admiten_null() {
            assertThatCode(() -> valido().consultation(null).observations(null).build())
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
                    arguments("type null", (ThrowingCallable) () -> valido().type(null).build(),
                            "type is required"),
                    arguments("product null",
                            (ThrowingCallable) () -> valido().product(null).build(),
                            "product is required"),
                    arguments("product vacio",
                            (ThrowingCallable) () -> valido().product("").build(),
                            "product is required"),
                    arguments("product en blanco",
                            (ThrowingCallable) () -> valido().product("   ").build(),
                            "product is required"),
                    arguments("product de 201 chars",
                            (ThrowingCallable) () -> valido().product("x".repeat(201)).build(),
                            "product must be 200 chars or less"),
                    arguments("dosage null", (ThrowingCallable) () -> valido().dosage(null).build(),
                            "dosage is required"),
                    arguments("dosage vacio", (ThrowingCallable) () -> valido().dosage("").build(),
                            "dosage is required"),
                    arguments("dosage en blanco",
                            (ThrowingCallable) () -> valido().dosage("   ").build(),
                            "dosage is required"),
                    arguments("dosage de 201 chars",
                            (ThrowingCallable) () -> valido().dosage("x".repeat(201)).build(),
                            "dosage must be 200 chars or less"),
                    arguments("observations de 2001 chars",
                            (ThrowingCallable) () -> valido().observations("x".repeat(2001))
                                    .build(),
                            "observations must be 2000 chars or less"),
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
        @ValueSource(ints = {1, 200})
        @DisplayName("product en el limite exacto se acepta")
        void product_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().product("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 200})
        @DisplayName("dosage en el limite exacto se acepta")
        void dosage_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().dosage("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("observations de 2000 chars justo en el limite se acepta")
        void observations_de_2000_chars_se_acepta() {
            assertThatCode(() -> valido().observations("x".repeat(2000)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            Deworming deworming = valido().build();

            valido().date(LocalDate.of(2026, 5, 1)).type(DewormingType.EXTERNAL).product("Bravecto")
                    .dosage("1 comprimido").animal(DewormingMother.MICHI).consultation(null)
                    .company(DewormingMother.OTRA_CLINICA).applyTo(deworming);

            assertThat(deworming.getDate()).isEqualTo(LocalDate.of(2026, 5, 1));
            assertThat(deworming.getType()).isEqualTo(DewormingType.EXTERNAL);
            assertThat(deworming.getProduct()).isEqualTo("Bravecto");
            assertThat(deworming.getDosage()).isEqualTo("1 comprimido");
            assertThat(deworming.getAnimal()).isEqualTo(DewormingMother.MICHI);
            assertThat(deworming.getConsultation()).isNull();
            assertThat(deworming.getCompany()).isEqualTo(DewormingMother.OTRA_CLINICA);
            assertThat(deworming.getId()).isEqualTo(1L);
            assertThat(deworming.getCreatedDate()).isEqualTo(DewormingMother.CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            Deworming deworming = valido().build();

            // El producto es valido y la empresa no: si validate() no corriera ANTES de
            // asignar, la desparasitacion se quedaria con el producto nuevo y la empresa
            // vieja.
            assertThatThrownBy(() -> valido().product("Bravecto").company(null).applyTo(deworming))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(deworming.getProduct()).isEqualTo("Drontal Plus");
            assertThat(deworming.getCompany()).isEqualTo(DewormingMother.CLINICA);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            Deworming deworming = valido().build();
            deworming.disable();

            valido().product("Bravecto").applyTo(deworming);

            assertThat(deworming.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Deworming deworming = valido().build();

            deworming.disable();
            assertThat(deworming.isEnabled()).isFalse();
            deworming.disable();
            assertThat(deworming.isEnabled()).isFalse();

            deworming.enable();
            assertThat(deworming.isEnabled()).isTrue();
            deworming.enable();
            assertThat(deworming.isEnabled()).isTrue();
        }
    }
}
