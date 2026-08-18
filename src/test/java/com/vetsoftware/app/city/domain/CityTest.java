package com.vetsoftware.app.city.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.city.testsupport.CityMother;
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

@DisplayName("City — invariantes y ciclo de vida del agregado")
class CityTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir cinco
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valida() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = CityMother.CITY_ID;
        private String name = "Medellin";
        private StateRef state = CityMother.ANTIOQUIA;
        private String daneCode = "05001";

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder state(StateRef v) {
            this.state = v;
            return this;
        }

        private Builder daneCode(String v) {
            this.daneCode = v;
            return this;
        }

        private City build() {
            return new City(id, name, state, daneCode, CityMother.CREADO, true);
        }

        private void applyTo(City city) {
            city.update(name, state, daneCode);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            City ciudad = valida().build();

            assertThat(ciudad.getId()).isEqualTo(CityMother.CITY_ID);
            assertThat(ciudad.getName()).isEqualTo("Medellin");
            assertThat(ciudad.getState()).isEqualTo(CityMother.ANTIOQUIA);
            assertThat(ciudad.getDaneCode()).isEqualTo("05001");
            assertThat(ciudad.getCreatedDate()).isEqualTo(CityMother.CREADO);
            assertThat(ciudad.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitada y con la fecha actual")
        void create_nace_sin_id_habilitada_y_con_la_fecha_actual() {
            City ciudad = City.create("Medellin", CityMother.ANTIOQUIA, "05001");

            assertThat(ciudad.getId()).isNull();
            assertThat(ciudad.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(ciudad.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("daneCode nulo se acepta: no es obligatorio")
        void dane_code_nulo_se_acepta() {
            assertThatCode(() -> valida().daneCode(null).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas en el constructor")
    class InvariantesConstructor {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null", (ThrowingCallable) () -> valida().name(null).build(),
                            "name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> valida().name("").build(),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> valida().name("   ").build(),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> valida().name("x".repeat(101)).build(),
                            "name must be 100 chars or less"),
                    arguments("state null", (ThrowingCallable) () -> valida().state(null).build(),
                            "state is required"));
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
            assertThatCode(() -> valida().name("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza name, state y daneCode y conserva id, createdDate y enabled")
        void reemplaza_los_campos_mutables_y_conserva_el_resto() {
            City ciudad = valida().build();

            valida().name("Envigado").state(CityMother.OTRO_ESTADO).daneCode("05266")
                    .applyTo(ciudad);

            assertThat(ciudad.getName()).isEqualTo("Envigado");
            assertThat(ciudad.getState()).isEqualTo(CityMother.OTRO_ESTADO);
            assertThat(ciudad.getDaneCode()).isEqualTo("05266");
            assertThat(ciudad.getId()).isEqualTo(CityMother.CITY_ID);
            assertThat(ciudad.getCreatedDate()).isEqualTo(CityMother.CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            City ciudad = valida().build();

            // El nombre es valido y el departamento no: si la validacion no corriera
            // ANTES de asignar, la ciudad se quedaria con el nombre nuevo y el
            // departamento viejo.
            assertThatThrownBy(() -> valida().name("Envigado").state(null).applyTo(ciudad))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(ciudad.getName()).isEqualTo("Medellin");
            assertThat(ciudad.getState()).isEqualTo(CityMother.ANTIOQUIA);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            City ciudad = valida().build();
            ciudad.disable();

            valida().applyTo(ciudad);

            assertThat(ciudad.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas en update")
    class InvariantesUpdate {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null",
                            (ThrowingCallable) () -> valida().build().update(null,
                                    CityMother.ANTIOQUIA, "05001"),
                            "name is required"),
                    arguments("name vacio",
                            (ThrowingCallable) () -> valida().build().update("",
                                    CityMother.ANTIOQUIA, "05001"),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> valida().build().update("   ",
                                    CityMother.ANTIOQUIA, "05001"),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> valida().build().update("x".repeat(101),
                                    CityMother.ANTIOQUIA, "05001"),
                            "name must be 100 chars or less"),
                    arguments("state null", (ThrowingCallable) () -> valida().build()
                            .update("Envigado", null, "05001"), "state is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("update rechaza")
        void update_rechaza(String caso, ThrowingCallable ejecucion, String mensaje) {
            assertThatThrownBy(ejecucion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            City ciudad = valida().build();

            ciudad.disable();
            assertThat(ciudad.isEnabled()).isFalse();
            ciudad.disable();
            assertThat(ciudad.isEnabled()).isFalse();

            ciudad.enable();
            assertThat(ciudad.isEnabled()).isTrue();
            ciudad.enable();
            assertThat(ciudad.isEnabled()).isTrue();
        }
    }
}
