package com.vetsoftware.app.spa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("SpaTypeRef — companion VO de spa")
class SpaTypeRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor compacto conserva cada campo")
        void el_constructor_compacto_conserva_cada_campo() {
            SpaTypeRef ref = new SpaTypeRef(20L, "Baño básico");

            assertThat(ref.id()).isEqualTo(20L);
            assertThat(ref.name()).isEqualTo("Baño básico");
        }

        @Test
        @DisplayName("dos refs con los mismos valores son iguales")
        void dos_refs_con_los_mismos_valores_son_iguales() {
            assertThat(new SpaTypeRef(20L, "Baño básico"))
                    .isEqualTo(new SpaTypeRef(20L, "Baño básico"));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    Arguments.of("id null", (Runnable) () -> new SpaTypeRef(null, "Baño básico"),
                            "spa type id is required"),
                    Arguments.of("name null", (Runnable) () -> new SpaTypeRef(20L, null),
                            "spa type name is required"),
                    Arguments.of("name vacio", (Runnable) () -> new SpaTypeRef(20L, ""),
                            "spa type name is required"),
                    Arguments.of("name en blanco", (Runnable) () -> new SpaTypeRef(20L, "   "),
                            "spa type name is required"));
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
