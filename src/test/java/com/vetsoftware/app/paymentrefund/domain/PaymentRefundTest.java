package com.vetsoftware.app.paymentrefund.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Estos casos usan fechas reales de 2026 -Semana Santa y un puente de la Ley
 * Emiliani- para que el plazo que se afirma sea el que un cliente esperaria de
 * verdad, no una aproximacion de solo fin de semana.
 */
@DisplayName("PaymentRefund — plazo de retracto contra el calendario real (#796)")
class PaymentRefundTest {

    private static final Long EMPRESA = 3L;
    private static final Long PAGO = 8100L;
    private static final BigDecimal IMPORTE = new BigDecimal("500000.00");
    private static final Long FIRMANTE = 990L;

    @Nested
    @DisplayName("Semana Santa 2026: Jueves (2 de abril) y Viernes Santo (3 de abril)")
    class SemanaSanta2026 {

        /** Miercoles 1 de abril de 2026, ultimo dia habil antes del Triduo. */
        private static final LocalDateTime COBRO = LocalDateTime.of(2026, 4, 1, 9, 0);

        /**
         * Quinto dia habil real: 2,3 (Triduo), 4,5 (fin de semana) no cuentan; 6,7,8,9,
         * 10 si. Contando solo fin de semana el plazo habria vencido el miercoles 8,
         * dos dias antes de lo que la ley concede.
         */
        private static final LocalDateTime QUINTO_DIA_HABIL_REAL = LocalDateTime.of(2026, 4, 10, 23,
                59, 59, 999_999_999);

        @Test
        @DisplayName("un retracto el jueves 9 de abril sigue vivo, aunque el conteo sin festivos ya lo hubiera vencido")
        void retracto_que_solo_vive_gracias_al_triduo_se_registra() {
            PaymentRefund refund = PaymentRefund.register(pago(COBRO), BigDecimal.ZERO, null,
                    IMPORTE, RefundMethod.CARD, "TARJ-1", LocalDateTime.of(2026, 4, 9, 16, 0),
                    LocalDate.of(2026, 4, 13), RefundReasonCode.WITHDRAWAL,
                    "El cliente ejerce su derecho de retracto", FIRMANTE, "req-semana-santa",
                    LocalDateTime.of(2026, 4, 9, 16, 0), QUINTO_DIA_HABIL_REAL);

            assertThat(refund.getReasonCode()).isEqualTo(RefundReasonCode.WITHDRAWAL);
        }

        @Test
        @DisplayName("un retracto tras el cierre del viernes 10 de abril ya vencio")
        void retracto_despues_del_quinto_dia_habil_real_se_rechaza() {
            assertThatThrownBy(() -> PaymentRefund.register(pago(COBRO), BigDecimal.ZERO, null,
                    IMPORTE, RefundMethod.CARD, "TARJ-1", LocalDateTime.of(2026, 4, 13, 8, 0),
                    LocalDate.of(2026, 4, 13), RefundReasonCode.WITHDRAWAL,
                    "El cliente ejerce su derecho de retracto", FIRMANTE, "req-semana-santa-tarde",
                    LocalDateTime.of(2026, 4, 13, 8, 0), QUINTO_DIA_HABIL_REAL))
                    .isInstanceOf(WithdrawalRefundPeriodExpiredException.class)
                    .hasMessageContaining("vencio el " + QUINTO_DIA_HABIL_REAL)
                    .satisfies(ex -> assertThat(
                            ((WithdrawalRefundPeriodExpiredException) ex).getPaymentId())
                            .isEqualTo(PAGO));
        }
    }

    @Nested
    @DisplayName("Puente de la Ley Emiliani: Asuncion trasladada al lunes 17 de agosto de 2026")
    class PuenteDeAsuncion {

        /** Jueves 13 de agosto de 2026, ultimo dia habil antes del puente. */
        private static final LocalDateTime COBRO = LocalDateTime.of(2026, 8, 13, 11, 0);

        /**
         * Quinto dia habil real: 14 (1), 15-16 fin de semana, 17 festivo trasladado, 18
         * (2), 19 (3), 20 (4), 21 (5). Sin el festivo el quinto habria caido el jueves
         * 20, un dia antes.
         */
        private static final LocalDateTime QUINTO_DIA_HABIL_REAL = LocalDateTime.of(2026, 8, 21, 23,
                59, 59, 999_999_999);

        @Test
        @DisplayName("un retracto el viernes 21 de agosto sigue vivo gracias al puente")
        void retracto_que_solo_vive_gracias_al_puente_se_registra() {
            PaymentRefund refund = PaymentRefund.register(pago(COBRO), BigDecimal.ZERO, null,
                    IMPORTE, RefundMethod.BANK_TRANSFER, "CTA-0099",
                    LocalDateTime.of(2026, 8, 21, 17, 30), LocalDate.of(2026, 8, 24),
                    RefundReasonCode.WITHDRAWAL, "El cliente ejerce su derecho de retracto",
                    FIRMANTE, "req-puente", LocalDateTime.of(2026, 8, 21, 17, 30),
                    QUINTO_DIA_HABIL_REAL);

            assertThat(refund.getRefundedAt()).isEqualTo(LocalDateTime.of(2026, 8, 21, 17, 30));
        }

        @Test
        @DisplayName("un retracto el lunes 24 de agosto ya vencio, puente incluido")
        void retracto_despues_del_puente_se_rechaza() {
            assertThatThrownBy(() -> PaymentRefund.register(pago(COBRO), BigDecimal.ZERO, null,
                    IMPORTE, RefundMethod.BANK_TRANSFER, "CTA-0099",
                    LocalDateTime.of(2026, 8, 24, 8, 0), LocalDate.of(2026, 8, 24),
                    RefundReasonCode.WITHDRAWAL, "El cliente ejerce su derecho de retracto",
                    FIRMANTE, "req-puente-tarde", LocalDateTime.of(2026, 8, 24, 8, 0),
                    QUINTO_DIA_HABIL_REAL))
                    .isInstanceOf(WithdrawalRefundPeriodExpiredException.class);
        }
    }

    @Nested
    @DisplayName("Validaciones del plazo")
    class ValidacionesDelPlazo {

        @Test
        @DisplayName("un retracto sin el plazo resuelto no se puede registrar")
        void un_retracto_sin_plazo_resuelto_explota() {
            SubscriptionPaymentRef pago = pago(LocalDateTime.of(2026, 4, 1, 9, 0));

            assertThatThrownBy(() -> PaymentRefund.register(pago, BigDecimal.ZERO, null, IMPORTE,
                    RefundMethod.CARD, "TARJ-1", LocalDateTime.of(2026, 4, 2, 9, 0),
                    LocalDate.of(2026, 4, 2), RefundReasonCode.WITHDRAWAL,
                    "El cliente ejerce su derecho de retracto", FIRMANTE, "req-sin-plazo",
                    LocalDateTime.of(2026, 4, 2, 9, 0), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("withdrawalDeadline");
        }

        @Test
        @DisplayName("un motivo distinto de WITHDRAWAL no exige ningun plazo")
        void otro_motivo_no_exige_plazo() {
            SubscriptionPaymentRef pago = pago(LocalDateTime.of(2020, 1, 1, 9, 0));

            PaymentRefund refund = PaymentRefund.register(pago, BigDecimal.ZERO, null, IMPORTE,
                    RefundMethod.CARD, "TARJ-1", LocalDateTime.of(2026, 4, 2, 9, 0),
                    LocalDate.of(2026, 4, 2), RefundReasonCode.BILLING_ERROR,
                    "Cobro duplicado de febrero", FIRMANTE, "req-sin-retracto",
                    LocalDateTime.of(2026, 4, 2, 9, 0), null);

            assertThat(refund.getReasonCode()).isEqualTo(RefundReasonCode.BILLING_ERROR);
        }
    }

    private static SubscriptionPaymentRef pago(LocalDateTime receivedAt) {
        return new SubscriptionPaymentRef(PAGO, EMPRESA, IMPORTE, receivedAt);
    }
}
