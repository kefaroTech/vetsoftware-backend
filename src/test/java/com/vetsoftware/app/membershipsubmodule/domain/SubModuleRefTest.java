package com.vetsoftware.app.membershipsubmodule.domain;

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

@DisplayName("SubModuleRef — companion VO con sus propias invariantes")
class SubModuleRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor compacto conserva cada campo")
        void el_constructor_compacto_conserva_cada_campo() {
            SubModuleRef ref = new SubModuleRef(980L, "Facturacion", "FACT");

            assertThat(ref.id()).isEqualTo(980L);
            assertThat(ref.name()).isEqualTo("Facturacion");
            assertThat(ref.code()).isEqualTo("FACT");
        }

        @Test
        @DisplayName("dos refs con los mismos valores son iguales")
        void dos_refs_con_los_mismos_valores_son_iguales() {
            SubModuleRef a = new SubModuleRef(980L, "Facturacion", "FACT");
            SubModuleRef b = new SubModuleRef(980L, "Facturacion", "FACT");

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("id null",
                            (ThrowingCallable) () -> new SubModuleRef(null, "Facturacion", "FACT"),
                            "subModule id is required"),
                    arguments("name null",
                            (ThrowingCallable) () -> new SubModuleRef(980L, null, "FACT"),
                            "subModule name is required"),
                    arguments("name vacio",
                            (ThrowingCallable) () -> new SubModuleRef(980L, "", "FACT"),
                            "subModule name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new SubModuleRef(980L, "   ", "FACT"),
                            "subModule name is required"),
                    arguments("code null",
                            (ThrowingCallable) () -> new SubModuleRef(980L, "Facturacion", null),
                            "subModule code is required"),
                    arguments("code vacio",
                            (ThrowingCallable) () -> new SubModuleRef(980L, "Facturacion", ""),
                            "subModule code is required"),
                    arguments("code en blanco",
                            (ThrowingCallable) () -> new SubModuleRef(980L, "Facturacion", "   "),
                            "subModule code is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor compacto rechaza")
        void el_constructor_compacto_rechaza(String caso, ThrowingCallable construccion,
                String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("name y code de un solo caracter se aceptan")
        void name_y_code_de_un_solo_caracter_se_aceptan() {
            assertThatCode(() -> new SubModuleRef(980L, "x", "y")).doesNotThrowAnyException();
        }
    }
}
