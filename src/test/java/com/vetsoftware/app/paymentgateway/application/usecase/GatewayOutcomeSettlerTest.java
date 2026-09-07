package com.vetsoftware.app.paymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayOutcomeSettler")
class GatewayOutcomeSettlerTest {

    private static final Long EMPRESA = 42L;
    private static final Long PAGO = 501L;
    private static final Long DOCUMENTO = 900L;
    private static final Long MEDIO_DE_PAGO = 15L;
    private static final BigDecimal MONTO = new BigDecimal("45000");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-04T08:15:30Z"),
            ZoneOffset.UTC);

    @Mock
    private SubscriptionPaymentLedgerPort ledgerPort;
    @Mock
    private PaymentAttemptRecorderPort attemptPort;
    @Mock
    private PaymentAttemptQueryPort attemptQueryPort;
    @Mock
    private PaymentGatewayMetrics metrics;

    private GatewayOutcomeSettler settler;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        settler = new GatewayOutcomeSettler(ledgerPort, attemptPort, attemptQueryPort, metrics,
                RELOJ);
    }

    @Test
    @DisplayName("APPROVED confirma el pago y no anota ningun intento")
    void approved_confirma() {
        FirstPeriodChargeOutcome outcome = settler.settle(GatewayTransactionStatus.APPROVED, null,
                EMPRESA, PAGO, DOCUMENTO, MEDIO_DE_PAGO, MONTO);

        assertThat(outcome).isEqualTo(FirstPeriodChargeOutcome.APPROVED);
        verify(ledgerPort).confirm(PAGO, EMPRESA);
        verify(attemptPort, never()).record(any(), any(), any(), any(), any(), any(), any(), any(),
                any());
        verify(metrics).recordChargeOutcome(FirstPeriodChargeOutcome.APPROVED, null);
    }

    @Nested
    @DisplayName("rechazos")
    class Rechazos {

        @Test
        @DisplayName("fondos insuficientes es SOFT y programa reintento a un dia")
        void fondos_insuficientes_es_soft() {
            FirstPeriodChargeOutcome outcome = settler.settle(GatewayTransactionStatus.DECLINED,
                    "Fondos insuficientes", EMPRESA, PAGO, DOCUMENTO, MEDIO_DE_PAGO, MONTO);

            assertThat(outcome).isEqualTo(FirstPeriodChargeOutcome.DECLINED);
            verify(ledgerPort).fail(PAGO, EMPRESA);
            ArgumentCaptor<LocalDateTime> nextAttempt = ArgumentCaptor
                    .forClass(LocalDateTime.class);
            verify(attemptPort).record(eq(EMPRESA), eq(DOCUMENTO), eq(MEDIO_DE_PAGO), eq("WOMPI"),
                    eq(MONTO), eq("Fondos insuficientes"), eq(GatewayDeclineKind.SOFT), any(),
                    nextAttempt.capture());
            assertThat(nextAttempt.getValue()).isEqualTo(
                    LocalDateTime.ofInstant(RELOJ.instant(), RELOJ.getZone()).plusDays(1));
            verify(metrics).recordChargeOutcome(FirstPeriodChargeOutcome.DECLINED,
                    GatewayDeclineKind.SOFT);
        }

        @Test
        @DisplayName("un tercer rechazo SOFT consulta la ventana de 14 dias y salta a la"
                + " escalera de RetrySchedule")
        void tercer_rechazo_soft_usa_la_escalera() {
            org.mockito.Mockito
                    .when(attemptQueryPort.countRetryableSince(eq(EMPRESA), eq(DOCUMENTO), any()))
                    .thenReturn(2);

            settler.settle(GatewayTransactionStatus.DECLINED, "Fondos insuficientes", EMPRESA, PAGO,
                    DOCUMENTO, MEDIO_DE_PAGO, MONTO);

            ArgumentCaptor<LocalDateTime> nextAttempt = ArgumentCaptor
                    .forClass(LocalDateTime.class);
            verify(attemptPort).record(eq(EMPRESA), eq(DOCUMENTO), eq(MEDIO_DE_PAGO), eq("WOMPI"),
                    eq(MONTO), eq("Fondos insuficientes"), eq(GatewayDeclineKind.SOFT), any(),
                    nextAttempt.capture());
            assertThat(nextAttempt.getValue()).isEqualTo(
                    LocalDateTime.ofInstant(RELOJ.instant(), RELOJ.getZone()).plusDays(4));
        }

        @Test
        @DisplayName("tarjeta robada es HARD y no programa reintento")
        void tarjeta_robada_es_hard() {
            settler.settle(GatewayTransactionStatus.DECLINED, "Tarjeta robada", EMPRESA, PAGO,
                    DOCUMENTO, MEDIO_DE_PAGO, MONTO);

            verify(attemptPort).record(eq(EMPRESA), eq(DOCUMENTO), eq(MEDIO_DE_PAGO), eq("WOMPI"),
                    eq(MONTO), eq("Tarjeta robada"), eq(GatewayDeclineKind.HARD), any(), isNull());
        }

        @Test
        @DisplayName("ERROR es CONFIGURATION sin importar el mensaje")
        void error_es_configuration() {
            settler.settle(GatewayTransactionStatus.ERROR, "algo raro", EMPRESA, PAGO, DOCUMENTO,
                    MEDIO_DE_PAGO, MONTO);

            verify(attemptPort).record(eq(EMPRESA), eq(DOCUMENTO), eq(MEDIO_DE_PAGO), eq("WOMPI"),
                    eq(MONTO), eq("algo raro"), eq(GatewayDeclineKind.CONFIGURATION), any(),
                    isNull());
        }

        @Test
        @DisplayName("VOIDED sin mensaje usa el propio estado como codigo")
        void sin_mensaje_usa_el_estado() {
            settler.settle(GatewayTransactionStatus.VOIDED, null, EMPRESA, PAGO, DOCUMENTO,
                    MEDIO_DE_PAGO, MONTO);

            verify(attemptPort).record(eq(EMPRESA), eq(DOCUMENTO), eq(MEDIO_DE_PAGO), eq("WOMPI"),
                    eq(MONTO), eq("VOIDED"), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("idempotencia: el webhook y el sondeo compiten por el mismo pago (RES-21)")
    class Idempotencia {

        @Test
        @DisplayName("si el pago ya quedo CONFIRMED no vuelve a confirmar ni anota nada")
        void ya_confirmado_no_repite() {
            org.mockito.Mockito.when(ledgerPort.currentOutcome(PAGO, EMPRESA))
                    .thenReturn(Optional.of(FirstPeriodChargeOutcome.APPROVED));

            FirstPeriodChargeOutcome outcome = settler.settle(GatewayTransactionStatus.APPROVED,
                    null, EMPRESA, PAGO, DOCUMENTO, MEDIO_DE_PAGO, MONTO);

            assertThat(outcome).isEqualTo(FirstPeriodChargeOutcome.APPROVED);
            verify(ledgerPort, never()).confirm(any(), any());
            verify(ledgerPort, never()).fail(any(), any());
            verifyNoInteractions(attemptPort);
            verifyNoInteractions(metrics);
        }

        @Test
        @DisplayName("si el pago ya quedo FAILED no vuelve a fallar ni anota otro intento")
        void ya_fallado_no_repite() {
            org.mockito.Mockito.when(ledgerPort.currentOutcome(PAGO, EMPRESA))
                    .thenReturn(Optional.of(FirstPeriodChargeOutcome.DECLINED));

            FirstPeriodChargeOutcome outcome = settler.settle(GatewayTransactionStatus.APPROVED,
                    null, EMPRESA, PAGO, DOCUMENTO, MEDIO_DE_PAGO, MONTO);

            assertThat(outcome).isEqualTo(FirstPeriodChargeOutcome.DECLINED);
            verify(ledgerPort, never()).confirm(any(), any());
            verify(ledgerPort, never()).fail(any(), any());
            verifyNoInteractions(attemptPort);
            verifyNoInteractions(metrics);
        }
    }
}
