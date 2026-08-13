package com.vetsoftware.app.cashregister.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("CashMovement — el signo lo pone el tipo, nunca el monto")
class CashMovementTest {

    private static final BigDecimal MONTO = new BigDecimal("50000");

    private static CashMovement conMonto(BigDecimal amount) {
        return new CashMovement(1L, CashMovementType.SALE_IN, CashPaymentMethod.CASH, amount,
                CashReferenceType.POS_DOCUMENT, 9L, 7L, LocalDateTime.of(2026, 1, 15, 11, 30),
                null);
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("type null",
                            (ThrowingCallable) () -> new CashMovement(null, null,
                                    CashPaymentMethod.CASH, MONTO, CashReferenceType.MANUAL, null,
                                    7L, null, null),
                            "type is required"),
                    arguments("method null",
                            (ThrowingCallable) () -> new CashMovement(null,
                                    CashMovementType.MANUAL_IN, null, MONTO,
                                    CashReferenceType.MANUAL, null, 7L, null, null),
                            "method is required"),
                    arguments("amount null", (ThrowingCallable) () -> conMonto(null),
                            "amount must be positive"),
                    arguments("amount en cero", (ThrowingCallable) () -> conMonto(BigDecimal.ZERO),
                            "amount must be positive"),
                    arguments("amount negativo",
                            (ThrowingCallable) () -> conMonto(new BigDecimal("-1")),
                            "amount must be positive"),
                    arguments("referenceType null",
                            (ThrowingCallable) () -> new CashMovement(null,
                                    CashMovementType.MANUAL_IN, CashPaymentMethod.CASH, MONTO, null,
                                    null, 7L, null, null),
                            "referenceType is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("un monto negativo no es una salida: las salidas se declaran con el tipo")
        void un_monto_negativo_no_es_una_salida() {
            // Si se aceptara un monto negativo, un retiro podria entrar como -X con tipo
            // de entrada y sumar en vez de restar. El monto siempre es positivo.
            assertThatThrownBy(() -> conMonto(new BigDecimal("-50000")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("signedAmount")
    class Signo {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = CashMovementType.class, names = {"SALE_IN", "OPEN_ACCOUNT_IN",
                "MANUAL_IN"})
        @DisplayName("las entradas suman el monto tal cual")
        void las_entradas_suman(CashMovementType tipo) {
            CashMovement movimiento = new CashMovement(1L, tipo, CashPaymentMethod.CASH, MONTO,
                    CashReferenceType.MANUAL, null, 7L, null, null);

            assertThat(movimiento.signedAmount()).isEqualByComparingTo("50000");
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = CashMovementType.class, names = {"WITHDRAWAL", "EXPENSE", "VOID_OUT"})
        @DisplayName("las salidas restan el monto")
        void las_salidas_restan(CashMovementType tipo) {
            CashMovement movimiento = new CashMovement(1L, tipo, CashPaymentMethod.CASH, MONTO,
                    CashReferenceType.MANUAL, null, 7L, null, null);

            assertThat(movimiento.signedAmount()).isEqualByComparingTo("-50000");
        }

        @Test
        @DisplayName("el monto guardado no cambia de signo: solo su aporte al total")
        void el_monto_guardado_no_cambia_de_signo() {
            CashMovement retiro = new CashMovement(1L, CashMovementType.WITHDRAWAL,
                    CashPaymentMethod.CASH, MONTO, CashReferenceType.MANUAL, null, 7L, null, null);

            assertThat(retiro.getAmount()).isEqualByComparingTo("50000");
            assertThat(retiro.signedAmount()).isEqualByComparingTo("-50000");
        }
    }

    @Nested
    @DisplayName("create y asignacion de id")
    class Creacion {

        @Test
        @DisplayName("nace sin id y con la fecha del momento")
        void nace_sin_id_y_con_la_fecha_del_momento() {
            CashMovement movimiento = CashMovement.create(CashMovementType.MANUAL_IN,
                    CashPaymentMethod.CASH, MONTO, CashReferenceType.MANUAL, null, 7L, "Sencillo");

            assertThat(movimiento.getId()).isNull();
            assertThat(movimiento.getNote()).isEqualTo("Sencillo");
            assertThat(movimiento.getReferenceId()).isNull();
            // createdAt lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(movimiento.getCreatedAt()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("assignId fija el id que devolvio la base")
        void assign_id_fija_el_id_de_la_base() {
            CashMovement movimiento = CashMovement.create(CashMovementType.MANUAL_IN,
                    CashPaymentMethod.CASH, MONTO, CashReferenceType.MANUAL, null, 7L, null);

            movimiento.assignId(42L);

            assertThat(movimiento.getId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("conserva la referencia al documento de origen")
        void conserva_la_referencia_al_documento_de_origen() {
            CashMovement movimiento = CashMovement.create(CashMovementType.SALE_IN,
                    CashPaymentMethod.CARD, MONTO, CashReferenceType.POS_DOCUMENT, 88L, 7L, null);

            // Tipo + id de referencia son la clave con la que la orquestacion deduplica.
            assertThat(movimiento.getReferenceType()).isEqualTo(CashReferenceType.POS_DOCUMENT);
            assertThat(movimiento.getReferenceId()).isEqualTo(88L);
            assertThat(movimiento.getCreatedByEmployeeId()).isEqualTo(7L);
        }
    }
}
