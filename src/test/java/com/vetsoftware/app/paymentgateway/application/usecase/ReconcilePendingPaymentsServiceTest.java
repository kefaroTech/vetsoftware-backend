package com.vetsoftware.app.paymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.dto.ReconciliationBatchResult;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics.FailureKind;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayPort;
import com.vetsoftware.app.paymentgateway.application.port.out.StalePendingPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransaction;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.paymentgateway.domain.StalePendingPayment;
import com.vetsoftware.app.paymentgateway.domain.WompiGatewayException;
import com.vetsoftware.app.paymentgateway.domain.WompiRateLimitedException;
import io.micrometer.observation.ObservationRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconcilePendingPaymentsService")
class ReconcilePendingPaymentsServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-04T10:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime CORTE = LocalDateTime.now(RELOJ).minusMinutes(60);

    @Mock
    private StalePendingPaymentQueryPort stalePendingPaymentQueryPort;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort;
    @Mock
    private GatewayOutcomeSettler outcomeSettler;
    @Mock
    private PaymentAttemptRecorderPort paymentAttemptRecorderPort;
    @Mock
    private PaymentAttemptQueryPort paymentAttemptQueryPort;
    @Mock
    private PaymentGatewayMetrics metrics;

    private ReconcilePendingPaymentsService service;

    private void montarServicio() {
        lenient().when(paymentGatewayPort.pendingTransactionMaxAge()) // no todos los casos llegan
                                                                      // al umbral de antiguedad
                .thenReturn(Duration.ofHours(24));
        service = new ReconcilePendingPaymentsService(stalePendingPaymentQueryPort,
                paymentGatewayPort, subscriptionPaymentLedgerPort, outcomeSettler,
                paymentAttemptRecorderPort, paymentAttemptQueryPort, ObservationRegistry.NOOP,
                RELOJ, metrics);
    }

    @Test
    @DisplayName("APPROVED: liquida con GatewayOutcomeSettler y cuenta como resuelto")
    void aprobado_liquida_y_cuenta_resuelto() {
        montarServicio();
        StalePendingPayment candidato = new StalePendingPayment(EMPRESA, 501L, "tx-1",
                new BigDecimal("45000"), "VS-DOC-900-A1", CORTE);
        when(stalePendingPaymentQueryPort.findOlderThan(CORTE, 50)).thenReturn(List.of(candidato));
        when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.APPROVED, null, "VS-DOC-900-A1", 4500000L, "CARD", null));
        when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, 501L))
                .thenReturn(Optional.of(900L));
        when(outcomeSettler.settle(GatewayTransactionStatus.APPROVED, null, EMPRESA, 501L, 900L,
                null, new BigDecimal("45000"))).thenReturn(FirstPeriodChargeOutcome.APPROVED);

        ReconciliationBatchResult result = service.reconcileOlderThan(CORTE, 50);

        assertThat(result).isEqualTo(new ReconciliationBatchResult(1, 1, 0));
        verify(outcomeSettler).settle(GatewayTransactionStatus.APPROVED, null, EMPRESA, 501L, 900L,
                null, new BigDecimal("45000"));
    }

    @Test
    @DisplayName("sigue PENDING en Wompi y no supera la antiguedad maxima: no liquida y no cuenta como fallo")
    void sigue_pendiente_no_liquida() {
        montarServicio();
        StalePendingPayment candidato = new StalePendingPayment(EMPRESA, 501L, "tx-1",
                new BigDecimal("45000"), "VS-DOC-900-A1", CORTE);
        when(stalePendingPaymentQueryPort.findOlderThan(CORTE, 50)).thenReturn(List.of(candidato));
        when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.PENDING, null, "VS-DOC-900-A1", 4500000L, "CARD", null));

        ReconciliationBatchResult result = service.reconcileOlderThan(CORTE, 50);

        assertThat(result).isEqualTo(new ReconciliationBatchResult(1, 0, 0));
        verify(subscriptionPaymentLedgerPort, never()).findDocumentIdByPayment(eq(EMPRESA),
                eq(501L));
        verify(subscriptionPaymentLedgerPort, never()).fail(any(), any());
    }

    @Test
    @DisplayName("PENDING que Wompi sostiene mas alla del umbral: anota un SOFT y sigue PENDING (RES2-29)")
    void pendiente_demasiado_viejo_anota_soft_sin_fallar() {
        montarServicio();
        LocalDateTime reservadoHaceDosDias = LocalDateTime.now(RELOJ).minusHours(49);
        StalePendingPayment candidato = new StalePendingPayment(EMPRESA, 501L, "tx-1",
                new BigDecimal("45000"), "VS-DOC-900-A1", reservadoHaceDosDias);
        when(stalePendingPaymentQueryPort.findOlderThan(CORTE, 50)).thenReturn(List.of(candidato));
        when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.PENDING, null, "VS-DOC-900-A1", 4500000L, "CARD", null));
        when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, 501L))
                .thenReturn(Optional.of(900L));
        when(paymentAttemptQueryPort.findLast(EMPRESA, 900L)).thenReturn(Optional.empty());
        when(paymentAttemptQueryPort.countRetryableSince(eq(EMPRESA), eq(900L), any()))
                .thenReturn(2);

        ReconciliationBatchResult result = service.reconcileOlderThan(CORTE, 50);

        assertThat(result).isEqualTo(new ReconciliationBatchResult(1, 1, 0));
        verify(subscriptionPaymentLedgerPort, never()).fail(any(), any());
        verify(paymentAttemptRecorderPort).record(EMPRESA, 900L, null, PaymentGatewayNames.WOMPI,
                new BigDecimal("45000"), null, GatewayDeclineKind.SOFT, LocalDateTime.now(RELOJ),
                LocalDateTime.now(RELOJ).plusDays(4));
        verifyNoInteractions(outcomeSettler);
    }

    @Test
    @DisplayName("PENDING ya marcado envejecido en una pasada anterior: no repite el intento")
    void pendiente_ya_marcado_envejecido_no_repite_intento() {
        montarServicio();
        LocalDateTime reservadoHaceDosDias = LocalDateTime.now(RELOJ).minusHours(49);
        StalePendingPayment candidato = new StalePendingPayment(EMPRESA, 501L, "tx-1",
                new BigDecimal("45000"), "VS-DOC-900-A1", reservadoHaceDosDias);
        when(stalePendingPaymentQueryPort.findOlderThan(CORTE, 50)).thenReturn(List.of(candidato));
        when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.PENDING, null, "VS-DOC-900-A1", 4500000L, "CARD", null));
        when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, 501L))
                .thenReturn(Optional.of(900L));
        when(paymentAttemptQueryPort.findLast(EMPRESA, 900L)).thenReturn(
                Optional.of(new com.vetsoftware.app.paymentgateway.domain.LastPaymentAttempt(1,
                        GatewayDeclineKind.SOFT, reservadoHaceDosDias.plusHours(1), null)));

        ReconciliationBatchResult result = service.reconcileOlderThan(CORTE, 50);

        assertThat(result).isEqualTo(new ReconciliationBatchResult(1, 0, 0));
        verifyNoInteractions(paymentAttemptRecorderPort);
        verify(subscriptionPaymentLedgerPort, never()).fail(any(), any());
    }

    @Test
    @DisplayName("DECLINED sin documento resuelto: liquida con documentId nulo")
    void declinado_sin_documento_resuelto() {
        montarServicio();
        StalePendingPayment candidato = new StalePendingPayment(EMPRESA, 501L, "tx-1",
                new BigDecimal("45000"), "VS-DOC-900-A1", CORTE);
        when(stalePendingPaymentQueryPort.findOlderThan(CORTE, 50)).thenReturn(List.of(candidato));
        when(paymentGatewayPort.findTransaction("tx-1"))
                .thenReturn(new GatewayTransaction("tx-1", GatewayTransactionStatus.DECLINED,
                        "insufficient_funds", "VS-DOC-900-A1", 4500000L, "CARD", null));
        when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, 501L))
                .thenReturn(Optional.empty());
        when(outcomeSettler.settle(GatewayTransactionStatus.DECLINED, "insufficient_funds", EMPRESA,
                501L, null, null, new BigDecimal("45000")))
                .thenReturn(FirstPeriodChargeOutcome.DECLINED);

        ReconciliationBatchResult result = service.reconcileOlderThan(CORTE, 50);

        assertThat(result).isEqualTo(new ReconciliationBatchResult(1, 1, 0));
        verify(outcomeSettler).settle(eq(GatewayTransactionStatus.DECLINED),
                eq("insufficient_funds"), eq(EMPRESA), eq(501L), isNull(), isNull(),
                eq(new BigDecimal("45000")));
    }

    @Test
    @DisplayName("importe distinto al registrado: falla con CONFIGURATION en vez de quedar PENDING para siempre (RES2-31)")
    void importe_distinto_falla_con_configuration() {
        montarServicio();
        StalePendingPayment candidato = new StalePendingPayment(EMPRESA, 501L, "tx-1",
                new BigDecimal("45000"), "VS-DOC-900-A1", CORTE);
        when(stalePendingPaymentQueryPort.findOlderThan(CORTE, 50)).thenReturn(List.of(candidato));
        when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.APPROVED, null, "VS-DOC-900-A1", 4000000L, "CARD", null));
        when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, 501L))
                .thenReturn(Optional.of(900L));

        ReconciliationBatchResult result = service.reconcileOlderThan(CORTE, 50);

        assertThat(result).isEqualTo(new ReconciliationBatchResult(1, 1, 0));
        verifyNoInteractions(outcomeSettler);
        verify(subscriptionPaymentLedgerPort).fail(501L, EMPRESA);
        verify(paymentAttemptRecorderPort).record(EMPRESA, 900L, null, PaymentGatewayNames.WOMPI,
                new BigDecimal("45000"), null, GatewayDeclineKind.CONFIGURATION,
                LocalDateTime.now(RELOJ), LocalDateTime.now(RELOJ).plusDays(1));
        verify(metrics).recordCollectionFailure(FailureKind.DETERMINISTIC);
    }

    @Test
    @DisplayName("un candidato envenenado lanza RuntimeException: cuenta como fallo y sigue con el resto (RES2-30)")
    void candidato_envenenado_cuenta_como_fallo_y_sigue() {
        montarServicio();
        StalePendingPayment falla = new StalePendingPayment(EMPRESA, 501L, "tx-1",
                new BigDecimal("45000"), "VS-DOC-900-A1", CORTE);
        StalePendingPayment aprueba = new StalePendingPayment(EMPRESA, 502L, "tx-2",
                new BigDecimal("30000"), "VS-DOC-901-A1", CORTE);
        when(stalePendingPaymentQueryPort.findOlderThan(CORTE, 50))
                .thenReturn(List.of(falla, aprueba));
        when(paymentGatewayPort.findTransaction("tx-1"))
                .thenThrow(new IllegalArgumentException("estado desconocido de Wompi"));
        when(paymentGatewayPort.findTransaction("tx-2")).thenReturn(new GatewayTransaction("tx-2",
                GatewayTransactionStatus.APPROVED, null, "VS-DOC-901-A1", 3000000L, "CARD", null));
        when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, 502L))
                .thenReturn(Optional.of(901L));
        when(outcomeSettler.settle(GatewayTransactionStatus.APPROVED, null, EMPRESA, 502L, 901L,
                null, new BigDecimal("30000"))).thenReturn(FirstPeriodChargeOutcome.APPROVED);

        ReconciliationBatchResult result = service.reconcileOlderThan(CORTE, 50);

        assertThat(result).isEqualTo(new ReconciliationBatchResult(2, 1, 1));
        verify(metrics).recordCollectionFailure(FailureKind.DETERMINISTIC);
    }

    @Test
    @DisplayName("Wompi limita la tasa (RES2-28): detiene el lote sin tocar el resto de candidatos")
    void wompi_limita_la_tasa_detiene_el_lote() {
        montarServicio();
        StalePendingPayment primero = new StalePendingPayment(EMPRESA, 501L, "tx-1",
                new BigDecimal("45000"), "VS-DOC-900-A1", CORTE);
        StalePendingPayment segundo = new StalePendingPayment(EMPRESA, 502L, "tx-2",
                new BigDecimal("30000"), "VS-DOC-901-A1", CORTE);
        when(stalePendingPaymentQueryPort.findOlderThan(CORTE, 50))
                .thenReturn(List.of(primero, segundo));
        when(paymentGatewayPort.findTransaction("tx-1"))
                .thenThrow(new WompiRateLimitedException("429", Duration.ofMinutes(30)));

        ReconciliationBatchResult result = service.reconcileOlderThan(CORTE, 50);

        assertThat(result).isEqualTo(new ReconciliationBatchResult(0, 0, 0));
        verify(paymentGatewayPort, never()).findTransaction("tx-2");
        verify(metrics).recordCollectionFailure(FailureKind.TRANSIENT);
    }

    @Test
    @DisplayName("fallo transitorio de la pasarela: cuenta como fallo y sigue con el resto del lote")
    void fallo_transitorio_cuenta_como_fallo_y_sigue() {
        montarServicio();
        StalePendingPayment falla = new StalePendingPayment(EMPRESA, 501L, "tx-1",
                new BigDecimal("45000"), "VS-DOC-900-A1", CORTE);
        StalePendingPayment aprueba = new StalePendingPayment(EMPRESA, 502L, "tx-2",
                new BigDecimal("30000"), "VS-DOC-901-A1", CORTE);
        when(stalePendingPaymentQueryPort.findOlderThan(CORTE, 50))
                .thenReturn(List.of(falla, aprueba));
        when(paymentGatewayPort.findTransaction("tx-1"))
                .thenThrow(new WompiGatewayException("timeout", new RuntimeException("timeout")));
        when(paymentGatewayPort.findTransaction("tx-2")).thenReturn(new GatewayTransaction("tx-2",
                GatewayTransactionStatus.APPROVED, null, "VS-DOC-901-A1", 3000000L, "CARD", null));
        when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, 502L))
                .thenReturn(Optional.of(901L));
        when(outcomeSettler.settle(GatewayTransactionStatus.APPROVED, null, EMPRESA, 502L, 901L,
                null, new BigDecimal("30000"))).thenReturn(FirstPeriodChargeOutcome.APPROVED);

        ReconciliationBatchResult result = service.reconcileOlderThan(CORTE, 50);

        assertThat(result).isEqualTo(new ReconciliationBatchResult(2, 1, 1));
        verify(metrics).recordCollectionFailure(FailureKind.TRANSIENT);
    }

    @Test
    @DisplayName("reserva sin referencia: Wompi SI tiene la transaccion por client_request_id, se asigna y se liquida")
    void reserva_sin_referencia_wompi_la_tiene() {
        montarServicio();
        StalePendingPayment reserva = new StalePendingPayment(EMPRESA, 501L, null,
                new BigDecimal("45000"), "VS-DOC-900-A1", CORTE);
        Instant creadaEnWompi = Instant.parse("2026-03-04T08:58:00Z");
        when(stalePendingPaymentQueryPort.findOlderThan(CORTE, 50)).thenReturn(List.of(reserva));
        when(paymentGatewayPort.findByReference("VS-DOC-900-A1")).thenReturn(Optional
                .of(new GatewayTransaction("tx-recuperada", GatewayTransactionStatus.APPROVED, null,
                        "VS-DOC-900-A1", 4500000L, "CARD", creadaEnWompi)));
        when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, 501L))
                .thenReturn(Optional.of(900L));
        when(outcomeSettler.settle(GatewayTransactionStatus.APPROVED, null, EMPRESA, 501L, 900L,
                null, new BigDecimal("45000"))).thenReturn(FirstPeriodChargeOutcome.APPROVED);

        ReconciliationBatchResult result = service.reconcileOlderThan(CORTE, 50);

        assertThat(result).isEqualTo(new ReconciliationBatchResult(1, 1, 0));
        verify(subscriptionPaymentLedgerPort).assignGatewayReference(501L, EMPRESA, "tx-recuperada",
                LocalDateTime.ofInstant(creadaEnWompi, RELOJ.getZone()));
        verify(outcomeSettler).settle(GatewayTransactionStatus.APPROVED, null, EMPRESA, 501L, 900L,
                null, new BigDecimal("45000"));
        verify(subscriptionPaymentLedgerPort, never()).fail(any(), any());
    }

    @Test
    @DisplayName("reserva sin referencia (#776/#787): Wompi tampoco la tiene, falla con CONFIGURATION reprogramado a +1 dia")
    void reserva_sin_referencia_falla_directo() {
        montarServicio();
        StalePendingPayment reserva = new StalePendingPayment(EMPRESA, 501L, null,
                new BigDecimal("45000"), "VS-DOC-900-A1", CORTE);
        when(stalePendingPaymentQueryPort.findOlderThan(CORTE, 50)).thenReturn(List.of(reserva));
        when(paymentGatewayPort.findByReference("VS-DOC-900-A1")).thenReturn(Optional.empty());
        when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, 501L))
                .thenReturn(Optional.of(900L));

        ReconciliationBatchResult result = service.reconcileOlderThan(CORTE, 50);

        assertThat(result).isEqualTo(new ReconciliationBatchResult(1, 1, 0));
        verify(subscriptionPaymentLedgerPort).fail(501L, EMPRESA);
        verify(paymentAttemptRecorderPort).record(EMPRESA, 900L, null, PaymentGatewayNames.WOMPI,
                new BigDecimal("45000"), null, GatewayDeclineKind.CONFIGURATION,
                LocalDateTime.now(RELOJ), LocalDateTime.now(RELOJ).plusDays(1));
        verifyNoInteractions(outcomeSettler);
    }
}
