package com.vetsoftware.app.role.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("CompanyRef — invariantes del companion VO")
class CompanyRefTest {

    @Test
    @DisplayName("conserva cada campo")
    void conserva_cada_campo() {
        CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900");

        assertThat(ref.id()).isEqualTo(9L);
        assertThat(ref.name()).isEqualTo("Clinica Norte");
        assertThat(ref.identifier()).isEqualTo("NIT-900");
    }

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(
                arguments("id null",
                        (ThrowingCallable) () -> new CompanyRef(null, "Clinica Norte", "NIT-900"),
                        "company id is required"),
                arguments("name null", (ThrowingCallable) () -> new CompanyRef(9L, null, "NIT-900"),
                        "company name is required"),
                arguments("name vacio", (ThrowingCallable) () -> new CompanyRef(9L, "", "NIT-900"),
                        "company name is required"),
                arguments("name en blanco",
                        (ThrowingCallable) () -> new CompanyRef(9L, "   ", "NIT-900"),
                        "company name is required"),
                arguments("identifier null",
                        (ThrowingCallable) () -> new CompanyRef(9L, "Clinica Norte", null),
                        "company identifier is required"),
                arguments("identifier vacio",
                        (ThrowingCallable) () -> new CompanyRef(9L, "Clinica Norte", ""),
                        "company identifier is required"),
                arguments("identifier en blanco",
                        (ThrowingCallable) () -> new CompanyRef(9L, "Clinica Norte", "   "),
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
