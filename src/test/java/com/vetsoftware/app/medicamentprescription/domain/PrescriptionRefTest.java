package com.vetsoftware.app.medicamentprescription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDate;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("PrescriptionRef — companion VO de la receta")
class PrescriptionRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("conserva id y fecha")
        void conserva_id_y_fecha() {
            LocalDate fecha = LocalDate.of(2026, 1, 10);

            PrescriptionRef ref = new PrescriptionRef(1L, fecha);

            assertThat(ref.id()).isEqualTo(1L);
            assertThat(ref.date()).isEqualTo(fecha);
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("id null",
                            (ThrowingCallable) () -> new PrescriptionRef(null,
                                    LocalDate.of(2026, 1, 10)),
                            "prescription id is required"),
                    arguments("date null", (ThrowingCallable) () -> new PrescriptionRef(1L, null),
                            "prescription date is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor compacto rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }
}
