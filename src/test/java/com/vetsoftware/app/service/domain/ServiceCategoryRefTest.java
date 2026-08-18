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

@DisplayName("ServiceCategoryRef — companion VO de categoria de servicio")
class ServiceCategoryRefTest {

    @Test
    @DisplayName("conserva cada campo en su sitio")
    void conserva_cada_campo_en_su_sitio() {
        ServiceCategoryRef ref = new ServiceCategoryRef(20L, "Consultas");

        assertThat(ref.id()).isEqualTo(20L);
        assertThat(ref.name()).isEqualTo("Consultas");
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("id null",
                            (ThrowingCallable) () -> new ServiceCategoryRef(null, "Consultas"),
                            "serviceCategory id is required"),
                    arguments("name null",
                            (ThrowingCallable) () -> new ServiceCategoryRef(20L, null),
                            "serviceCategory name is required"),
                    arguments("name vacio",
                            (ThrowingCallable) () -> new ServiceCategoryRef(20L, ""),
                            "serviceCategory name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new ServiceCategoryRef(20L, "   "),
                            "serviceCategory name is required"));
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
