package com.vetsoftware.app.paymentattempt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentAttempt")
class PaymentAttemptTest {

    private static final Long EMPRESA = 900L;
    private static final Long DOCUMENTO = 8400L;
    private static final Long MEDIO_DE_PAGO = 8410L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 20, 12, 0, 0);

    private static PaymentAttempt intento(DeclineKind clase) {
        String codigo = clase == DeclineKind.CONFIGURATION ? null : "insufficient_funds";
        return PaymentAttempt.attempted(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO, 1, "wompi",
                new BigDecimal("119000.00"), codigo, clase, AHORA, null, AHORA);
    }

    @Nested
    @DisplayName("Reprogramar (RES-45)")
    class Reprogramar {

        @Test
        @DisplayName("mueve el siguiente intento cuando es posterior al propio")
        void mueve_el_siguiente_intento() {
            PaymentAttempt soft = intento(DeclineKind.SOFT);

            soft.reschedule(AHORA.plusDays(2));

            assertThat(soft.getNextAttemptAt()).isEqualTo(AHORA.plusDays(2));
        }

        @Test
        @DisplayName("sin fecha, revienta")
        void sin_fecha_revienta() {
            assertThatThrownBy(() -> intento(DeclineKind.SOFT).reschedule(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("anterior al propio intento, revienta")
        void anterior_al_intento_revienta() {
            assertThatThrownBy(() -> intento(DeclineKind.SOFT).reschedule(AHORA.minusHours(1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("un rechazo duro tambien se reprograma: lo dispara fijar una tarjeta nueva")
        void un_rechazo_duro_se_reprograma() {
            PaymentAttempt duro = intento(DeclineKind.HARD);

            duro.reschedule(AHORA.plusDays(1));

            assertThat(duro.getNextAttemptAt()).isEqualTo(AHORA.plusDays(1));
        }
    }

    @Nested
    @DisplayName("Presupuesto de reintentos del cliente")
    class PresupuestoDeReintentos {

        @Test
        @DisplayName("CONFIGURATION no consume el presupuesto del cliente")
        void configuration_no_consume() {
            assertThat(intento(DeclineKind.CONFIGURATION).consumesCustomerAttempts()).isFalse();
        }

        @Test
        @DisplayName("SOFT y HARD si consumen el presupuesto del cliente")
        void soft_y_hard_consumen() {
            assertThat(intento(DeclineKind.SOFT).consumesCustomerAttempts()).isTrue();
            assertThat(intento(DeclineKind.HARD).consumesCustomerAttempts()).isTrue();
        }
    }

    @Nested
    @DisplayName("Invariantes del constructor")
    class InvariantesDelConstructor {

        @Test
        @DisplayName("un rechazo duro con reintento programado ya se construye: RES-45 retiro la restriccion")
        void un_rechazo_duro_con_reintento_se_construye() {
            PaymentAttempt duro = new PaymentAttempt(1L, EMPRESA, DOCUMENTO, MEDIO_DE_PAGO, 1,
                    "wompi", new BigDecimal("119000.00"), "lost_card", DeclineKind.HARD, AHORA,
                    AHORA.plusDays(2), AHORA, 0L);

            assertThat(duro.getNextAttemptAt()).isEqualTo(AHORA.plusDays(2));
        }

        @Test
        @DisplayName("sin companyId, revienta")
        void sin_company_id_revienta() {
            assertThatThrownBy(() -> PaymentAttempt.attempted(null, DOCUMENTO, MEDIO_DE_PAGO, 1,
                    "wompi", new BigDecimal("119000.00"), "insufficient_funds", DeclineKind.SOFT,
                    AHORA, null, AHORA)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
