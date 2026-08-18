package com.vetsoftware.app.submodule.domain;

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

@DisplayName("ModuleRef — companion VO de referencia a module")
class ModuleRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor compacto conserva cada campo")
        void el_constructor_compacto_conserva_cada_campo() {
            ModuleRef ref = new ModuleRef(1L, "Facturacion", "FACT");

            assertThat(ref.id()).isEqualTo(1L);
            assertThat(ref.name()).isEqualTo("Facturacion");
            assertThat(ref.code()).isEqualTo("FACT");
        }

        @Test
        @DisplayName("dos refs con los mismos valores son iguales")
        void dos_refs_con_los_mismos_valores_son_iguales() {
            ModuleRef a = new ModuleRef(1L, "Facturacion", "FACT");
            ModuleRef b = new ModuleRef(1L, "Facturacion", "FACT");

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("id null",
                            (ThrowingCallable) () -> new ModuleRef(null, "Facturacion", "FACT"),
                            "module id is required"),
                    arguments("name null", (ThrowingCallable) () -> new ModuleRef(1L, null, "FACT"),
                            "module name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> new ModuleRef(1L, "", "FACT"),
                            "module name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new ModuleRef(1L, "   ", "FACT"),
                            "module name is required"),
                    arguments("code null",
                            (ThrowingCallable) () -> new ModuleRef(1L, "Facturacion", null),
                            "module code is required"),
                    arguments("code vacio",
                            (ThrowingCallable) () -> new ModuleRef(1L, "Facturacion", ""),
                            "module code is required"),
                    arguments("code en blanco",
                            (ThrowingCallable) () -> new ModuleRef(1L, "Facturacion", "   "),
                            "module code is required"));
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
