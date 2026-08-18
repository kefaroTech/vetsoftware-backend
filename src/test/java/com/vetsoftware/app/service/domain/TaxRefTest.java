package com.vetsoftware.app.service.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("TaxRef — companion VO de impuesto")
class TaxRefTest {

    @Nested
    @DisplayName("construccion valida")
    class Construccion {

        @Test
        @DisplayName("conserva cada campo en su sitio")
        void conserva_cada_campo_en_su_sitio() {
            TaxRef ref = new TaxRef(30L, "IVA 19%", new BigDecimal("19.00"));

            assertThat(ref.id()).isEqualTo(30L);
            assertThat(ref.name()).isEqualTo("IVA 19%");
            assertThat(ref.percentage()).isEqualByComparingTo("19.00");
        }

        @Test
        @DisplayName("un porcentaje de cero es un impuesto valido")
        void porcentaje_cero_es_valido() {
            assertThatCode(() -> new TaxRef(30L, "IVA 0%", BigDecimal.ZERO))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("id null",
                            (ThrowingCallable) () -> new TaxRef(null, "IVA", BigDecimal.TEN),
                            "tax id is required"),
                    arguments("name null",
                            (ThrowingCallable) () -> new TaxRef(30L, null, BigDecimal.TEN),
                            "tax name is required"),
                    arguments("name vacio",
                            (ThrowingCallable) () -> new TaxRef(30L, "", BigDecimal.TEN),
                            "tax name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new TaxRef(30L, "   ", BigDecimal.TEN),
                            "tax name is required"),
                    arguments("percentage null",
                            (ThrowingCallable) () -> new TaxRef(30L, "IVA", null),
                            "tax percentage is required"),
                    arguments("percentage negativo", (ThrowingCallable) () -> new TaxRef(30L, "IVA",
                            new BigDecimal("-0.01")), "tax percentage cannot be negative"));
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
