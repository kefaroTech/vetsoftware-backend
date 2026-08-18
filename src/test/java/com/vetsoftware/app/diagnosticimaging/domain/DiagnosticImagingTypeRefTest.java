package com.vetsoftware.app.diagnosticimaging.domain;

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

@DisplayName("DiagnosticImagingTypeRef — invariantes del companion VO")
class DiagnosticImagingTypeRefTest {

    @Test
    @DisplayName("construye con todos los campos en su sitio")
    void construye_con_todos_los_campos() {
        DiagnosticImagingTypeRef ref = new DiagnosticImagingTypeRef(1L, "Radiografia");

        assertThat(ref.id()).isEqualTo(1L);
        assertThat(ref.name()).isEqualTo("Radiografia");
    }

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(
                arguments("id null",
                        (ThrowingCallable) () -> new DiagnosticImagingTypeRef(null, "Radiografia"),
                        "diagnostic imaging type id is required"),
                arguments("name null",
                        (ThrowingCallable) () -> new DiagnosticImagingTypeRef(1L, null),
                        "diagnostic imaging type name is required"),
                arguments("name en blanco",
                        (ThrowingCallable) () -> new DiagnosticImagingTypeRef(1L, "   "),
                        "diagnostic imaging type name is required"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosInvalidos")
    @DisplayName("el constructor compacto rechaza")
    void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
        assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(mensaje);
    }
}
