package com.vetsoftware.app.promotion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("CompanyRef — VO de la empresa dueña de la promocion")
class CompanyRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("conserva los tres campos")
        void conserva_los_tres_campos() {
            CompanyRef ref = new CompanyRef(5L, "Veterinaria de prueba", "900123456");

            assertThat(ref.id()).isEqualTo(5L);
            assertThat(ref.name()).isEqualTo("Veterinaria de prueba");
            assertThat(ref.identifier()).isEqualTo("900123456");
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("id null",
                            (ThrowingCallable) () -> new CompanyRef(null, "Clinica", "900"),
                            "company id is required"),
                    arguments("name null", (ThrowingCallable) () -> new CompanyRef(5L, null, "900"),
                            "company name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> new CompanyRef(5L, "", "900"),
                            "company name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new CompanyRef(5L, "   ", "900"),
                            "company name is required"),
                    arguments("identifier null",
                            (ThrowingCallable) () -> new CompanyRef(5L, "Clinica", null),
                            "company identifier is required"),
                    arguments("identifier vacio",
                            (ThrowingCallable) () -> new CompanyRef(5L, "Clinica", ""),
                            "company identifier is required"),
                    arguments("identifier en blanco",
                            (ThrowingCallable) () -> new CompanyRef(5L, "Clinica", "   "),
                            "company identifier is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor compacto rechaza")
        void el_constructor_compacto_rechaza(String caso, ThrowingCallable construccion,
                String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }
}
