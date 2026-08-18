package com.vetsoftware.app.prescription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDate;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("ConsultationRef — invariantes del companion VO")
class ConsultationRefTest {

    @Test
    @DisplayName("construye con todos los campos en su sitio")
    void construye_con_todos_los_campos() {
        ConsultationRef ref = new ConsultationRef(1L, LocalDate.of(2026, 1, 10));

        assertThat(ref.id()).isEqualTo(1L);
        assertThat(ref.date()).isEqualTo(LocalDate.of(2026, 1, 10));
    }

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(
                arguments("id null",
                        (ThrowingCallable) () -> new ConsultationRef(null,
                                LocalDate.of(2026, 1, 10)),
                        "consultation id is required"),
                arguments("date null", (ThrowingCallable) () -> new ConsultationRef(1L, null),
                        "consultation date is required"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosInvalidos")
    @DisplayName("el constructor compacto rechaza")
    void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
        assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(mensaje);
    }
}
