package com.vetsoftware.app.breed.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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

@DisplayName("Breed — invariantes y ciclo de vida del agregado")
class BreedTest {

    private static final SpecieRef PERRO = new SpecieRef(1L, "Perro");
    private static final SpecieRef GATO = new SpecieRef(2L, "Gato");
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static Builder valido() {
        return new Builder();
    }

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir los
     * cinco argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static final class Builder {
        private Long id = 1L;
        private String name = "Labrador";
        private SpecieRef specie = PERRO;
        private LocalDateTime createdDate = CREADO;
        private boolean enabled = true;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder specie(SpecieRef v) {
            this.specie = v;
            return this;
        }

        private Breed build() {
            return new Breed(id, name, specie, createdDate, null, enabled);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Breed breed = valido().build();

            assertThat(breed.getId()).isEqualTo(1L);
            assertThat(breed.getName()).isEqualTo("Labrador");
            assertThat(breed.getSpecie()).isEqualTo(PERRO);
            assertThat(breed.getCreatedDate()).isEqualTo(CREADO);
            assertThat(breed.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con la especie dada")
        void create_nace_sin_id_habilitado_y_con_la_especie_dada() {
            Breed breed = Breed.create("Labrador", PERRO);

            assertThat(breed.getId()).isNull();
            assertThat(breed.getName()).isEqualTo("Labrador");
            assertThat(breed.getSpecie()).isEqualTo(PERRO);
            assertThat(breed.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(breed.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 100})
        @DisplayName("name en el limite exacto se acepta")
        void name_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().name("x".repeat(longitud)).build())
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
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> valido().name("x".repeat(101)).build(),
                            "name must be 100 chars or less"),
                    arguments("specie null", (ThrowingCallable) () -> valido().specie(null).build(),
                            "specie is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null",
                            (java.util.function.Consumer<Breed>) b -> b.update(null, PERRO),
                            "name is required"),
                    arguments("name vacio",
                            (java.util.function.Consumer<Breed>) b -> b.update("", PERRO),
                            "name is required"),
                    arguments("name de 101 chars",
                            (java.util.function.Consumer<Breed>) b -> b.update("x".repeat(101),
                                    PERRO),
                            "name must be 100 chars or less"),
                    arguments("specie null",
                            (java.util.function.Consumer<Breed>) b -> b.update("Golden", null),
                            "specie is required"));
        }

        @Test
        @DisplayName("reemplaza nombre y especie, conserva id, createdDate y enabled")
        void reemplaza_nombre_y_especie_conserva_el_resto() {
            Breed breed = valido().build();

            breed.update("Golden", GATO);

            assertThat(breed.getName()).isEqualTo("Golden");
            assertThat(breed.getSpecie()).isEqualTo(GATO);
            assertThat(breed.getId()).isEqualTo(1L);
            assertThat(breed.getCreatedDate()).isEqualTo(CREADO);
            assertThat(breed.isEnabled()).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza los mismos invariantes que el constructor y no deja el agregado a medias")
        void rechaza_invariantes_sin_dejar_el_agregado_a_medias(String caso,
                java.util.function.Consumer<Breed> actualizacion, String mensaje) {
            Breed breed = valido().build();

            assertThatThrownBy(() -> actualizacion.accept(breed))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);

            assertThat(breed.getName()).isEqualTo("Labrador");
            assertThat(breed.getSpecie()).isEqualTo(PERRO);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Breed breed = valido().build();

            breed.disable();
            assertThat(breed.isEnabled()).isFalse();
            breed.disable();
            assertThat(breed.isEnabled()).isFalse();

            breed.enable();
            assertThat(breed.isEnabled()).isTrue();
            breed.enable();
            assertThat(breed.isEnabled()).isTrue();
        }
    }
}
