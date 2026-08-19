package com.vetsoftware.app.animalcolor.domain;

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

@DisplayName("AnimalColor — invariantes y ciclo de vida del agregado")
class AnimalColorTest {

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
        private String name = "Negro";
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

        private AnimalColor build() {
            return new AnimalColor(id, name, specie, createdDate, null, enabled);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            AnimalColor color = valido().build();

            assertThat(color.getId()).isEqualTo(1L);
            assertThat(color.getName()).isEqualTo("Negro");
            assertThat(color.getSpecie()).isEqualTo(PERRO);
            assertThat(color.getCreatedDate()).isEqualTo(CREADO);
            assertThat(color.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con la especie dada")
        void create_nace_sin_id_habilitado_y_con_la_especie_dada() {
            AnimalColor color = AnimalColor.create("Negro", PERRO);

            assertThat(color.getId()).isNull();
            assertThat(color.getName()).isEqualTo("Negro");
            assertThat(color.getSpecie()).isEqualTo(PERRO);
            assertThat(color.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(color.getCreatedDate()).isCloseTo(LocalDateTime.now(),
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
                            (java.util.function.Consumer<AnimalColor>) c -> c.update(null, PERRO),
                            "name is required"),
                    arguments("name vacio",
                            (java.util.function.Consumer<AnimalColor>) c -> c.update("", PERRO),
                            "name is required"),
                    arguments("name de 101 chars",
                            (java.util.function.Consumer<AnimalColor>) c -> c
                                    .update("x".repeat(101), PERRO),
                            "name must be 100 chars or less"),
                    arguments("specie null", (java.util.function.Consumer<AnimalColor>) c -> c
                            .update("Blanco", null), "specie is required"));
        }

        @Test
        @DisplayName("reemplaza nombre y especie, conserva id, createdDate y enabled")
        void reemplaza_nombre_y_especie_conserva_el_resto() {
            AnimalColor color = valido().build();

            color.update("Blanco", GATO);

            assertThat(color.getName()).isEqualTo("Blanco");
            assertThat(color.getSpecie()).isEqualTo(GATO);
            assertThat(color.getId()).isEqualTo(1L);
            assertThat(color.getCreatedDate()).isEqualTo(CREADO);
            assertThat(color.isEnabled()).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza los mismos invariantes que el constructor y no deja el agregado a medias")
        void rechaza_invariantes_sin_dejar_el_agregado_a_medias(String caso,
                java.util.function.Consumer<AnimalColor> actualizacion, String mensaje) {
            AnimalColor color = valido().build();

            assertThatThrownBy(() -> actualizacion.accept(color))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);

            assertThat(color.getName()).isEqualTo("Negro");
            assertThat(color.getSpecie()).isEqualTo(PERRO);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            AnimalColor color = valido().build();

            color.disable();
            assertThat(color.isEnabled()).isFalse();
            color.disable();
            assertThat(color.isEnabled()).isFalse();

            color.enable();
            assertThat(color.isEnabled()).isTrue();
            color.enable();
            assertThat(color.isEnabled()).isTrue();
        }
    }
}
