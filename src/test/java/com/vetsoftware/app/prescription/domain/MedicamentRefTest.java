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

@DisplayName("MedicamentRef — invariantes del companion VO")
class MedicamentRefTest {

    private static MedicamentRef valido() {
        return new MedicamentRef(1L, "Amoxicilina", "Tableta", 2.0, "Cada 12 horas",
                "Con alimento");
    }

    @Test
    @DisplayName("construye con todos los campos en su sitio")
    void construye_con_todos_los_campos() {
        MedicamentRef ref = valido();

        assertThat(ref.id()).isEqualTo(1L);
        assertThat(ref.name()).isEqualTo("Amoxicilina");
        assertThat(ref.presentation()).isEqualTo("Tableta");
        assertThat(ref.quantity()).isEqualTo(2.0);
        assertThat(ref.posology()).isEqualTo("Cada 12 horas");
        assertThat(ref.observation()).isEqualTo("Con alimento");
    }

    @Test
    @DisplayName("observation es opcional")
    void observation_es_opcional() {
        assertThatCode(
                () -> new MedicamentRef(1L, "Amoxicilina", "Tableta", 2.0, "Cada 12 horas", null))
                .doesNotThrowAnyException();
    }

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(
                arguments("id null",
                        (ThrowingCallable) () -> new MedicamentRef(null, "Amoxicilina", "Tableta",
                                2.0, "Cada 12 horas", null),
                        "medicament id is required"),
                arguments("name null",
                        (ThrowingCallable) () -> new MedicamentRef(1L, null, "Tableta", 2.0,
                                "Cada 12 horas", null),
                        "medicament name is required"),
                arguments("name en blanco",
                        (ThrowingCallable) () -> new MedicamentRef(1L, "   ", "Tableta", 2.0,
                                "Cada 12 horas", null),
                        "medicament name is required"),
                arguments("presentation null",
                        (ThrowingCallable) () -> new MedicamentRef(1L, "Amoxicilina", null, 2.0,
                                "Cada 12 horas", null),
                        "medicament presentation is required"),
                arguments("presentation en blanco",
                        (ThrowingCallable) () -> new MedicamentRef(1L, "Amoxicilina", "   ", 2.0,
                                "Cada 12 horas", null),
                        "medicament presentation is required"),
                arguments("quantity null",
                        (ThrowingCallable) () -> new MedicamentRef(1L, "Amoxicilina", "Tableta",
                                null, "Cada 12 horas", null),
                        "medicament quantity is required"),
                arguments("posology null",
                        (ThrowingCallable) () -> new MedicamentRef(1L, "Amoxicilina", "Tableta",
                                2.0, null, null),
                        "medicament posology is required"),
                arguments(
                        "posology en blanco", (ThrowingCallable) () -> new MedicamentRef(1L,
                                "Amoxicilina", "Tableta", 2.0, "   ", null),
                        "medicament posology is required"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosInvalidos")
    @DisplayName("el constructor compacto rechaza")
    void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
        assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(mensaje);
    }
}
