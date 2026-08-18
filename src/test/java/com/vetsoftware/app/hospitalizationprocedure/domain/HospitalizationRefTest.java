package com.vetsoftware.app.hospitalizationprocedure.domain;

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

@DisplayName("HospitalizationRef — invariantes del value object")
class HospitalizationRefTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);

    @Test
    @DisplayName("el constructor compacto conserva id y fecha")
    void el_constructor_compacto_conserva_id_y_fecha() {
        HospitalizationRef ref = new HospitalizationRef(20L, FECHA);

        assertThat(ref.id()).isEqualTo(20L);
        assertThat(ref.date()).isEqualTo(FECHA);
    }

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(
                arguments("id null", (ThrowingCallable) () -> new HospitalizationRef(null, FECHA),
                        "hospitalization id is required"),
                arguments("date null", (ThrowingCallable) () -> new HospitalizationRef(20L, null),
                        "hospitalization date is required"));
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
