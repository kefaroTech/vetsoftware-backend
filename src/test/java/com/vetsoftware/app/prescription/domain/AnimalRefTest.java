package com.vetsoftware.app.prescription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("AnimalRef — invariantes del companion VO")
class AnimalRefTest {

    @Test
    @DisplayName("construye con todos los campos en su sitio")
    void construye_con_todos_los_campos() {
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

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(
                arguments("id null",
                        (ThrowingCallable) () -> new AnimalRef(null, "Firulais", "A-001"),
                        "animal id is required"),
                arguments("name null", (ThrowingCallable) () -> new AnimalRef(1L, null, "A-001"),
                        "animal name is required"),
                arguments("name en blanco",
                        (ThrowingCallable) () -> new AnimalRef(1L, "   ", "A-001"),
                        "animal name is required"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosInvalidos")
    @DisplayName("el constructor compacto rechaza")
    void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
        assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(mensaje);
    }
}
