package com.vetsoftware.app.specie.domain;

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

@DisplayName("Specie — invariantes y ciclo de vida del agregado")
class SpecieTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static Specie nuevaSpecie() {
        return new Specie(1L, "Perro", CREADO, null, true);
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Specie specie = nuevaSpecie();

            assertThat(specie.getId()).isEqualTo(1L);
            assertThat(specie.getName()).isEqualTo("Perro");
            assertThat(specie.getCreatedDate()).isEqualTo(CREADO);
            assertThat(specie.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id y habilitada")
        void create_nace_sin_id_y_habilitada() {
            Specie specie = Specie.create("Perro");

            assertThat(specie.getId()).isNull();
            assertThat(specie.getName()).isEqualTo("Perro");
            assertThat(specie.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion es una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(specie.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("invariantes del nombre — constructor")
    class InvariantesConstructor {

        static Stream<Arguments> nombresInvalidos() {
            return Stream.of(
                    arguments("null",
                            (ThrowingCallable) () -> new Specie(1L, null, CREADO, null, true),
                            "name is required"),
                    arguments("vacio",
                            (ThrowingCallable) () -> new Specie(1L, "", CREADO, null, true),
                            "name is required"),
                    arguments("en blanco",
                            (ThrowingCallable) () -> new Specie(1L, "   ", CREADO, null, true),
                            "name is required"),
                    arguments("101 chars", (ThrowingCallable) () -> new Specie(1L, "x".repeat(101),
                            CREADO, null, true), "name must be 100 chars or less"));
        }

        @ParameterizedTest(name = "name {0}")
        @MethodSource("nombresInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 100})
        @DisplayName("name en el limite exacto se acepta")
        void name_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> new Specie(1L, "x".repeat(longitud), CREADO, null, true))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza el nombre y conserva id, createdDate y habilitacion")
        void reemplaza_el_nombre_y_conserva_el_resto() {
            Specie specie = nuevaSpecie();

            specie.update("Gato");

            assertThat(specie.getName()).isEqualTo("Gato");
            assertThat(specie.getId()).isEqualTo(1L);
            assertThat(specie.getCreatedDate()).isEqualTo(CREADO);
            assertThat(specie.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            Specie specie = nuevaSpecie();
            specie.disable();

            specie.update("Gato");

            assertThat(specie.isEnabled()).isFalse();
        }

        static Stream<Arguments> nombresInvalidos() {
            return Stream.of(
                    arguments("null", (ThrowingCallable) () -> nuevaSpecie().update(null),
                            "name is required"),
                    arguments("vacio", (ThrowingCallable) () -> nuevaSpecie().update(""),
                            "name is required"),
                    arguments("en blanco", (ThrowingCallable) () -> nuevaSpecie().update("   "),
                            "name is required"),
                    arguments("101 chars",
                            (ThrowingCallable) () -> nuevaSpecie().update("x".repeat(101)),
                            "name must be 100 chars or less"));
        }

        @ParameterizedTest(name = "name {0}")
        @MethodSource("nombresInvalidos")
        @DisplayName("rechaza el mismo conjunto de nombres invalidos que el constructor")
        void rechaza_nombres_invalidos(String caso, ThrowingCallable actualizacion,
                String mensaje) {
            assertThatThrownBy(actualizacion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            Specie specie = nuevaSpecie();

            assertThatThrownBy(() -> specie.update(null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(specie.getName()).isEqualTo("Perro");
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Specie specie = nuevaSpecie();

            specie.disable();
            assertThat(specie.isEnabled()).isFalse();
            specie.disable();
            assertThat(specie.isEnabled()).isFalse();

            specie.enable();
            assertThat(specie.isEnabled()).isTrue();
            specie.enable();
            assertThat(specie.isEnabled()).isTrue();
        }
    }
}
