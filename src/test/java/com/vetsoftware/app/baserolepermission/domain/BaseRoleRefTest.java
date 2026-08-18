package com.vetsoftware.app.baserolepermission.domain;

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

@DisplayName("BaseRoleRef — companion VO de referencia a baserole")
class BaseRoleRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor compacto conserva cada campo")
        void el_constructor_compacto_conserva_cada_campo() {
            BaseRoleRef ref = new BaseRoleRef(1L, "Veterinario", "VET");

            assertThat(ref.id()).isEqualTo(1L);
            assertThat(ref.name()).isEqualTo("Veterinario");
            assertThat(ref.code()).isEqualTo("VET");
        }

        @Test
        @DisplayName("dos refs con los mismos valores son iguales")
        void dos_refs_con_los_mismos_valores_son_iguales() {
            BaseRoleRef a = new BaseRoleRef(1L, "Veterinario", "VET");
            BaseRoleRef b = new BaseRoleRef(1L, "Veterinario", "VET");

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }

        @Test
        @DisplayName("name y code de un solo caracter se aceptan")
        void name_y_code_de_un_solo_caracter_se_aceptan() {
            assertThatCode(() -> new BaseRoleRef(1L, "x", "y")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("id null",
                            (ThrowingCallable) () -> new BaseRoleRef(null, "Veterinario", "VET"),
                            "baseRole id is required"),
                    arguments("name null",
                            (ThrowingCallable) () -> new BaseRoleRef(1L, null, "VET"),
                            "baseRole name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> new BaseRoleRef(1L, "", "VET"),
                            "baseRole name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new BaseRoleRef(1L, "   ", "VET"),
                            "baseRole name is required"),
                    arguments("code null",
                            (ThrowingCallable) () -> new BaseRoleRef(1L, "Veterinario", null),
                            "baseRole code is required"),
                    arguments("code vacio",
                            (ThrowingCallable) () -> new BaseRoleRef(1L, "Veterinario", ""),
                            "baseRole code is required"),
                    arguments("code en blanco",
                            (ThrowingCallable) () -> new BaseRoleRef(1L, "Veterinario", "   "),
                            "baseRole code is required"));
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
}
