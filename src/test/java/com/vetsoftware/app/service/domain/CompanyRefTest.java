package com.vetsoftware.app.service.domain;

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

@DisplayName("CompanyRef — companion VO de empresa")
class CompanyRefTest {

    @Test
    @DisplayName("conserva cada campo en su sitio")
    void conserva_cada_campo_en_su_sitio() {
        CompanyRef ref = new CompanyRef(9L, "Veterinaria de prueba", "900123456");

        assertThat(ref.id()).isEqualTo(9L);
        assertThat(ref.name()).isEqualTo("Veterinaria de prueba");
        assertThat(ref.identifier()).isEqualTo("900123456");
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("id null",
                            (ThrowingCallable) () -> new CompanyRef(null, "Clinica", "900123456"),
                            "company id is required"),
                    arguments("name null",
                            (ThrowingCallable) () -> new CompanyRef(9L, null, "900123456"),
                            "company name is required"),
                    arguments("name vacio",
                            (ThrowingCallable) () -> new CompanyRef(9L, "", "900123456"),
                            "company name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new CompanyRef(9L, "   ", "900123456"),
                            "company name is required"),
                    arguments("identifier null",
                            (ThrowingCallable) () -> new CompanyRef(9L, "Clinica", null),
                            "company identifier is required"),
                    arguments("identifier vacio",
                            (ThrowingCallable) () -> new CompanyRef(9L, "Clinica", ""),
                            "company identifier is required"),
                    arguments("identifier en blanco",
                            (ThrowingCallable) () -> new CompanyRef(9L, "Clinica", "   "),
                            "company identifier is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }
}
