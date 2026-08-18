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

@DisplayName("BasePermissionRef — companion VO de referencia a basepermission")
class BasePermissionRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor compacto conserva cada campo")
        void el_constructor_compacto_conserva_cada_campo() {
            BasePermissionRef ref = new BasePermissionRef(10L, "Crear consulta", "CONSULTA_CREATE");

            assertThat(ref.id()).isEqualTo(10L);
            assertThat(ref.name()).isEqualTo("Crear consulta");
            assertThat(ref.code()).isEqualTo("CONSULTA_CREATE");
        }

        @Test
        @DisplayName("dos refs con los mismos valores son iguales")
        void dos_refs_con_los_mismos_valores_son_iguales() {
            BasePermissionRef a = new BasePermissionRef(10L, "Crear consulta", "CONSULTA_CREATE");
            BasePermissionRef b = new BasePermissionRef(10L, "Crear consulta", "CONSULTA_CREATE");

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }

        @Test
        @DisplayName("name y code de un solo caracter se aceptan")
        void name_y_code_de_un_solo_caracter_se_aceptan() {
            assertThatCode(() -> new BasePermissionRef(10L, "x", "y")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("id null",
                            (ThrowingCallable) () -> new BasePermissionRef(null, "Crear consulta",
                                    "CONSULTA_CREATE"),
                            "basePermission id is required"),
                    arguments("name null",
                            (ThrowingCallable) () -> new BasePermissionRef(10L, null,
                                    "CONSULTA_CREATE"),
                            "basePermission name is required"),
                    arguments("name vacio",
                            (ThrowingCallable) () -> new BasePermissionRef(10L, "",
                                    "CONSULTA_CREATE"),
                            "basePermission name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new BasePermissionRef(10L, "   ",
                                    "CONSULTA_CREATE"),
                            "basePermission name is required"),
                    arguments("code null",
                            (ThrowingCallable) () -> new BasePermissionRef(10L, "Crear consulta",
                                    null),
                            "basePermission code is required"),
                    arguments("code vacio",
                            (ThrowingCallable) () -> new BasePermissionRef(10L, "Crear consulta",
                                    ""),
                            "basePermission code is required"),
                    arguments("code en blanco", (ThrowingCallable) () -> new BasePermissionRef(10L,
                            "Crear consulta", "   "), "basePermission code is required"));
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
