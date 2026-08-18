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

@DisplayName("MembershipRef — companion VO con sus propias invariantes")
class MembershipRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor compacto conserva cada campo")
        void el_constructor_compacto_conserva_cada_campo() {
            MembershipRef ref = new MembershipRef(900L, "Plan Premium");

            assertThat(ref.id()).isEqualTo(900L);
            assertThat(ref.name()).isEqualTo("Plan Premium");
        }

        @Test
        @DisplayName("dos refs con los mismos valores son iguales")
        void dos_refs_con_los_mismos_valores_son_iguales() {
            MembershipRef a = new MembershipRef(900L, "Plan Premium");
            MembershipRef b = new MembershipRef(900L, "Plan Premium");

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("id null",
                            (ThrowingCallable) () -> new MembershipRef(null, "Plan Premium"),
                            "membership id is required"),
                    arguments("name null", (ThrowingCallable) () -> new MembershipRef(900L, null),
                            "membership name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> new MembershipRef(900L, ""),
                            "membership name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new MembershipRef(900L, "   "),
                            "membership name is required"));
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
        @DisplayName("name de un solo caracter se acepta")
        void name_de_un_solo_caracter_se_acepta() {
            assertThatCode(() -> new MembershipRef(900L, "x")).doesNotThrowAnyException();
        }
    }
}
