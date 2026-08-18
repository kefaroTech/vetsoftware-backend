package com.vetsoftware.app.medicamentprescription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

@DisplayName("MedicamentRef — companion VO del catalogo de medicamentos")
class MedicamentRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("conserva id y nombre")
        void conserva_id_y_nombre() {
            MedicamentRef ref = new MedicamentRef(1L, "Amoxicilina 500mg");

            assertThat(ref.id()).isEqualTo(1L);
            assertThat(ref.name()).isEqualTo("Amoxicilina 500mg");
        }

        @Test
        @DisplayName("un nombre de 200 caracteres, el limite exacto, se acepta")
        void nombre_de_200_caracteres_se_acepta() {
            assertThatCode(() -> new MedicamentRef(1L, "x".repeat(200))).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("id null", (ThrowingCallable) () -> new MedicamentRef(null, "Suero"),
                            "medicament id is required"),
                    arguments("name null", (ThrowingCallable) () -> new MedicamentRef(1L, null),
                            "medicament name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> new MedicamentRef(1L, ""),
                            "medicament name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new MedicamentRef(1L, "   "),
                            "medicament name is required"),
                    arguments("name de 201 caracteres",
                            (ThrowingCallable) () -> new MedicamentRef(1L, "x".repeat(201)),
                            "medicament name must be 200 chars or less"));
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
