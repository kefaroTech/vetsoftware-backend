package com.vetsoftware.app.spa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("AnimalRef — companion VO de spa")
class AnimalRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor compacto conserva cada campo")
        void el_constructor_compacto_conserva_cada_campo() {
            AnimalRef ref = new AnimalRef(1L, "Firulais", "A-001");

            assertThat(ref.id()).isEqualTo(1L);
            assertThat(ref.name()).isEqualTo("Firulais");
            assertThat(ref.code()).isEqualTo("A-001");
        }

        @Test
        @DisplayName("code es opcional")
        void code_es_opcional() {
            assertThatCode(() -> new AnimalRef(1L, "Firulais", null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("dos refs con los mismos valores son iguales")
        void dos_refs_con_los_mismos_valores_son_iguales() {
            assertThat(new AnimalRef(1L, "Firulais", "A-001"))
                    .isEqualTo(new AnimalRef(1L, "Firulais", "A-001"));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    Arguments.of("id null",
                            (Runnable) () -> new AnimalRef(null, "Firulais", "A-001"),
                            "animal id is required"),
                    Arguments.of("name null", (Runnable) () -> new AnimalRef(1L, null, "A-001"),
                            "animal name is required"),
                    Arguments.of("name vacio", (Runnable) () -> new AnimalRef(1L, "", "A-001"),
                            "animal name is required"),
                    Arguments.of("name en blanco",
                            (Runnable) () -> new AnimalRef(1L, "   ", "A-001"),
                            "animal name is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, Runnable construccion, String mensaje) {
            assertThatThrownBy(construccion::run).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }
}
