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

@DisplayName("CompanyRef — companion VO de spa")
class CompanyRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor compacto conserva cada campo")
        void el_constructor_compacto_conserva_cada_campo() {
            CompanyRef ref = new CompanyRef(10L, "Veterinaria de prueba", "900123456");

            assertThat(ref.id()).isEqualTo(10L);
            assertThat(ref.name()).isEqualTo("Veterinaria de prueba");
            assertThat(ref.identifier()).isEqualTo("900123456");
        }

        @Test
        @DisplayName("dos refs con los mismos valores son iguales")
        void dos_refs_con_los_mismos_valores_son_iguales() {
            assertThat(new CompanyRef(10L, "Clinica", "900"))
                    .isEqualTo(new CompanyRef(10L, "Clinica", "900"));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    Arguments.of("id null", (Runnable) () -> new CompanyRef(null, "Clinica", "900"),
                            "company id is required"),
                    Arguments.of("name null", (Runnable) () -> new CompanyRef(10L, null, "900"),
                            "company name is required"),
                    Arguments.of("name en blanco",
                            (Runnable) () -> new CompanyRef(10L, "   ", "900"),
                            "company name is required"),
                    Arguments.of("identifier null",
                            (Runnable) () -> new CompanyRef(10L, "Clinica", null),
                            "company identifier is required"),
                    Arguments.of("identifier en blanco",
                            (Runnable) () -> new CompanyRef(10L, "Clinica", "   "),
                            "company identifier is required"));
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
