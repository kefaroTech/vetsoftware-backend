package com.vetsoftware.app.vaccinationtype.domain;

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

@DisplayName("CompanyRef — companion VO con sus propias invariantes")
class CompanyRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor compacto conserva cada campo")
        void el_constructor_compacto_conserva_cada_campo() {
            CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900");

            assertThat(ref.id()).isEqualTo(9L);
            assertThat(ref.name()).isEqualTo("Clinica Norte");
            assertThat(ref.identifier()).isEqualTo("NIT-900");
        }

        @Test
        @DisplayName("dos refs con los mismos valores son iguales")
        void dos_refs_con_los_mismos_valores_son_iguales() {
            CompanyRef a = new CompanyRef(9L, "Clinica Norte", "NIT-900");
            CompanyRef b = new CompanyRef(9L, "Clinica Norte", "NIT-900");

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("id null",
                            (ThrowingCallable) () -> new CompanyRef(null, "Clinica Norte",
                                    "NIT-900"),
                            "company id is required"),
                    arguments("name null",
                            (ThrowingCallable) () -> new CompanyRef(9L, null, "NIT-900"),
                            "company name is required"),
                    arguments("name vacio",
                            (ThrowingCallable) () -> new CompanyRef(9L, "", "NIT-900"),
                            "company name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new CompanyRef(9L, "   ", "NIT-900"),
                            "company name is required"),
                    arguments("identifier null",
                            (ThrowingCallable) () -> new CompanyRef(9L, "Clinica Norte", null),
                            "company identifier is required"),
                    arguments("identifier vacio",
                            (ThrowingCallable) () -> new CompanyRef(9L, "Clinica Norte", ""),
                            "company identifier is required"),
                    arguments("identifier en blanco",
                            (ThrowingCallable) () -> new CompanyRef(9L, "Clinica Norte", "   "),
                            "company identifier is required"));
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
        @DisplayName("name y identifier de un solo caracter se aceptan")
        void name_e_identifier_de_un_solo_caracter_se_aceptan() {
            assertThatCode(() -> new CompanyRef(9L, "x", "y")).doesNotThrowAnyException();
        }
    }
}
