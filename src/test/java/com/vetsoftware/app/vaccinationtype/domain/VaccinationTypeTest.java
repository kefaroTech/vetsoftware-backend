package com.vetsoftware.app.vaccinationtype.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.vaccinationtype.testsupport.VaccinationTypeMother;
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

@DisplayName("VaccinationType — invariantes y ciclo de vida del agregado")
class VaccinationTypeTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir seis
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = VaccinationTypeMother.TYPE_ID;
        private String name = "Rabia";
        private String description = "Vacuna antirrabica";
        private CompanyRef company = VaccinationTypeMother.CLINICA;
        private boolean general = false;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder description(String v) {
            this.description = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Builder general(boolean v) {
            this.general = v;
            return this;
        }

        private VaccinationType build() {
            return new VaccinationType(id, name, description, company, general,
                    VaccinationTypeMother.CREADO, true);
        }

        private void applyTo(VaccinationType vaccinationType) {
            vaccinationType.update(name, description, company, general);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            VaccinationType tipo = valido().build();

            assertThat(tipo.getId()).isEqualTo(VaccinationTypeMother.TYPE_ID);
            assertThat(tipo.getName()).isEqualTo("Rabia");
            assertThat(tipo.getDescription()).isEqualTo("Vacuna antirrabica");
            assertThat(tipo.getCompany()).isEqualTo(VaccinationTypeMother.CLINICA);
            assertThat(tipo.isGeneral()).isFalse();
            assertThat(tipo.getCreatedDate()).isEqualTo(VaccinationTypeMother.CREADO);
            assertThat(tipo.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con la fecha actual")
        void create_nace_sin_id_habilitado_y_con_la_fecha_actual() {
            VaccinationType tipo = VaccinationType.create("Rabia", "Vacuna antirrabica",
                    VaccinationTypeMother.CLINICA, false);

            assertThat(tipo.getId()).isNull();
            assertThat(tipo.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(tipo.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("create() tambien acepta un tipo general sin compania")
        void create_tambien_acepta_un_tipo_general_sin_compania() {
            VaccinationType tipo = VaccinationType.create("Vacuna universal",
                    "Disponible para todas", null, true);

            assertThat(tipo.isGeneral()).isTrue();
            assertThat(tipo.getCompany()).isNull();
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
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> valido().name("x".repeat(101)).build(),
                            "name must be 100 chars or less"),
                    arguments("description de 501 chars",
                            (ThrowingCallable) () -> valido().description("x".repeat(501)).build(),
                            "description must be 500 chars or less"),
                    arguments("general con compania",
                            (ThrowingCallable) () -> valido().general(true).build(),
                            "general type cannot have company"),
                    arguments("no general sin compania",
                            (ThrowingCallable) () -> valido().company(null).build(),
                            "non-general type requires company"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 100})
        @DisplayName("name en el limite exacto se acepta")
        void name_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().name("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("description nula se acepta: es opcional")
        void description_nula_se_acepta() {
            assertThatCode(() -> valido().description(null).build()).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 500})
        @DisplayName("description en el limite exacto se acepta")
        void description_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().description("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("un tipo general sin compania se acepta")
        void un_tipo_general_sin_compania_se_acepta() {
            assertThatCode(() -> valido().general(true).company(null).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("un tipo no general con compania se acepta")
        void un_tipo_no_general_con_compania_se_acepta() {
            assertThatCode(
                    () -> valido().general(false).company(VaccinationTypeMother.CLINICA).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            VaccinationType tipo = valido().build();

            valido().name("Moquillo").description("Vacuna contra el moquillo")
                    .company(VaccinationTypeMother.OTRA_CLINICA).applyTo(tipo);

            assertThat(tipo.getName()).isEqualTo("Moquillo");
            assertThat(tipo.getDescription()).isEqualTo("Vacuna contra el moquillo");
            assertThat(tipo.getCompany()).isEqualTo(VaccinationTypeMother.OTRA_CLINICA);
            assertThat(tipo.getId()).isEqualTo(VaccinationTypeMother.TYPE_ID);
            assertThat(tipo.getCreatedDate()).isEqualTo(VaccinationTypeMother.CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            VaccinationType tipo = valido().build();

            // El nombre es valido y la compania (null, sin marcar general) no: si
            // validate()
            // no corriera ANTES de asignar, el tipo se quedaria con el nombre nuevo y la
            // compania vieja perdida.
            assertThatThrownBy(() -> valido().name("Moquillo").company(null).applyTo(tipo))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(tipo.getName()).isEqualTo("Rabia");
            assertThat(tipo.getCompany()).isEqualTo(VaccinationTypeMother.CLINICA);
        }

        @Test
        @DisplayName("update puede convertir un tipo propio en general liberando la compania")
        void update_puede_convertir_un_tipo_propio_en_general() {
            VaccinationType tipo = valido().build();

            valido().general(true).company(null).applyTo(tipo);

            assertThat(tipo.isGeneral()).isTrue();
            assertThat(tipo.getCompany()).isNull();
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            VaccinationType tipo = valido().build();
            tipo.disable();

            valido().applyTo(tipo);

            assertThat(tipo.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            VaccinationType tipo = valido().build();

            tipo.disable();
            assertThat(tipo.isEnabled()).isFalse();
            tipo.disable();
            assertThat(tipo.isEnabled()).isFalse();

            tipo.enable();
            assertThat(tipo.isEnabled()).isTrue();
            tipo.enable();
            assertThat(tipo.isEnabled()).isTrue();
        }
    }
}
