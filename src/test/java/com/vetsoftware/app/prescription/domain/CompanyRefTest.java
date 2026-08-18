package com.vetsoftware.app.prescription.domain;

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
    @DisplayName("construye con todos los campos en su sitio")
    void construye_con_todos_los_campos() {
        CompanyRef ref = new CompanyRef(1L, "Veterinaria Test", "900123456");

        assertThat(ref.id()).isEqualTo(1L);
        assertThat(ref.name()).isEqualTo("Veterinaria Test");
        assertThat(ref.identifier()).isEqualTo("900123456");
    }

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(
                arguments("id null",
                        (ThrowingCallable) () -> new CompanyRef(null, "Veterinaria Test",
                                "900123456"),
                        "company id is required"),
                arguments("name null",
                        (ThrowingCallable) () -> new CompanyRef(1L, null, "900123456"),
                        "company name is required"),
                arguments("name en blanco",
                        (ThrowingCallable) () -> new CompanyRef(1L, "   ", "900123456"),
                        "company name is required"),
                arguments("identifier null",
                        (ThrowingCallable) () -> new CompanyRef(1L, "Veterinaria Test", null),
                        "company identifier is required"),
                arguments("identifier en blanco",
                        (ThrowingCallable) () -> new CompanyRef(1L, "Veterinaria Test", "   "),
                        "company identifier is required"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosInvalidos")
    @DisplayName("el constructor compacto rechaza")
    void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
        assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(mensaje);
    }
}
