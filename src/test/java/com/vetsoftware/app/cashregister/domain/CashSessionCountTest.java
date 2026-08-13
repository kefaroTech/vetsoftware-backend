package com.vetsoftware.app.cashregister.domain;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("CashSessionCount — sobrante, faltante o cuadre del arqueo")
class CashSessionCountTest {

    @Nested
    @DisplayName("difference")
    class Diferencia {

        @ParameterizedTest(name = "esperado {0}, contado {1} → {2}")
        @CsvSource({"130000, 125000, -5000", "130000, 135000, 5000", "130000, 130000, 0",
                "0, 7000, 7000", "0, 0, 0"})
        @DisplayName("es contado menos esperado: positiva sobra, negativa falta")
        void es_contado_menos_esperado(String esperado, String contado, String diferencia) {
            CashSessionCount conteo = CashSessionCount.create(CashPaymentMethod.CASH,
                    new BigDecimal(esperado), new BigDecimal(contado));

            assertThat(conteo.difference()).isEqualByComparingTo(diferencia);
        }

        @Test
        @DisplayName("el conteo no altera lo esperado: guarda los dos lados del arqueo")
        void el_conteo_no_altera_lo_esperado() {
            CashSessionCount conteo = CashSessionCount.create(CashPaymentMethod.CASH,
                    new BigDecimal("130000"), new BigDecimal("125000"));

            // Los dos valores se persisten tal cual: el arqueo tiene que poder enseñar
            // que se esperaban 130.000 y aparecieron 125.000, no solo el faltante.
            assertThat(conteo.getExpectedAmount()).isEqualByComparingTo("130000");
            assertThat(conteo.getCountedAmount()).isEqualByComparingTo("125000");
            assertThat(conteo.getMethod()).isEqualTo(CashPaymentMethod.CASH);
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("method null",
                            (ThrowingCallable) () -> CashSessionCount.create(null, BigDecimal.ONE,
                                    BigDecimal.ONE),
                            "method is required"),
                    arguments("expectedAmount null",
                            (ThrowingCallable) () -> CashSessionCount.create(CashPaymentMethod.CASH,
                                    null, BigDecimal.ONE),
                            "expectedAmount is required"),
                    arguments(
                            "countedAmount null", (ThrowingCallable) () -> CashSessionCount
                                    .create(CashPaymentMethod.CASH, BigDecimal.ONE, null),
                            "countedAmount is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("un conteo negativo se acepta: el dominio no juzga lo que el cajero declara")
        void un_conteo_negativo_se_acepta() {
            // No hay invariante de signo a proposito: el arqueo registra lo declarado y
            // la diferencia es lo que se revisa despues.
            assertThat(CashSessionCount
                    .create(CashPaymentMethod.CASH, BigDecimal.ZERO, new BigDecimal("-100"))
                    .difference()).isEqualByComparingTo("-100");
        }
    }

    @Nested
    @DisplayName("create y asignacion de id")
    class Creacion {

        @Test
        @DisplayName("nace sin id y lo recibe al persistirse")
        void nace_sin_id_y_lo_recibe_al_persistirse() {
            CashSessionCount conteo = CashSessionCount.create(CashPaymentMethod.CARD,
                    BigDecimal.TEN, BigDecimal.TEN);

            assertThat(conteo.getId()).isNull();

            conteo.assignId(42L);

            assertThat(conteo.getId()).isEqualTo(42L);
        }
    }
}
