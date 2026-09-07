package com.vetsoftware.app.paymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.dto.GatewayChargeResult;
import com.vetsoftware.app.paymentgateway.application.port.out.CompanyBillingEmailQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayPort;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.domain.ChargeRequest;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.FiscalProfileNotConfiguredException;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransaction;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import com.vetsoftware.app.paymentgateway.domain.PaymentReservation;
import com.vetsoftware.app.paymentgateway.domain.PaymentReservationOutcome;
import com.vetsoftware.app.paymentgateway.domain.WompiGatewayException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayCharger")
class GatewayChargerTest {

    private static final Long EMPRESA = 42L;
    private static final Long DOCUMENTO = 900L;
    private static final PaymentMethodRef MEDIO_DE_PAGO = new PaymentMethodRef(15L, "9911");
    private static final String REFERENCIA = "VS-DOC-900-A1";
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-01-01T08:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private CompanyBillingEmailQueryPort companyBillingEmailQueryPort;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort;
    @Mock
    private GatewayOutcomeSettler outcomeSettler;
    @Mock
    private PaymentGatewayMetrics metrics;

    private GatewayCharger charger;

    @BeforeEach
    void setUp() {
        charger = new GatewayCharger(companyBillingEmailQueryPort, paymentGatewayPort,
                subscriptionPaymentLedgerPort, outcomeSettler, metrics, RELOJ);
    }

    private void conPerfilFiscalYTransaccionPendiente() {
        when(subscriptionPaymentLedgerPort.findByClientRequestId(EMPRESA, REFERENCIA))
                .thenReturn(Optional.empty());
        when(companyBillingEmailQueryPort.findFiscalEmail(EMPRESA))
                .thenReturn(Optional.of("facturacion@clinica.co"));
        when(paymentGatewayPort.charge(any())).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.PENDING, null, REFERENCIA, 4500000L, "CARD", null));
        when(subscriptionPaymentLedgerPort.registerAndApply(eq(EMPRESA),
                eq(new BigDecimal("45000")), eq("COP"), isNull(), any(), eq(REFERENCIA),
                eq(DOCUMENTO))).thenReturn(new PaymentReservationOutcome(501L, false));
    }

    @Test
    @DisplayName("referencia ya reservada por otra llamada: no cobra otra vez (#772)")
    void referencia_ya_reservada_no_llama_a_la_pasarela() {
        when(subscriptionPaymentLedgerPort.findByClientRequestId(EMPRESA, REFERENCIA))
                .thenReturn(Optional.of(new PaymentReservation(501L, "tx-1")));
        when(subscriptionPaymentLedgerPort.currentOutcome(501L, EMPRESA))
                .thenReturn(Optional.empty());

        GatewayChargeResult result = charger.charge(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO,
                new BigDecimal("45000"), "COP", REFERENCIA);

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.PENDING);
        assertThat(result.gatewayReference()).isEqualTo("tx-1");
        verifyNoInteractions(companyBillingEmailQueryPort, paymentGatewayPort, outcomeSettler);
    }

    @Test
    @DisplayName("referencia ya reservada y ya resuelta: devuelve el desenlace final sin sondear")
    void referencia_ya_reservada_y_resuelta_devuelve_el_desenlace() {
        when(subscriptionPaymentLedgerPort.findByClientRequestId(EMPRESA, REFERENCIA))
                .thenReturn(Optional.of(new PaymentReservation(501L, "tx-1")));
        when(subscriptionPaymentLedgerPort.currentOutcome(501L, EMPRESA))
                .thenReturn(Optional.of(FirstPeriodChargeOutcome.APPROVED));

        GatewayChargeResult result = charger.charge(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO,
                new BigDecimal("45000"), "COP", REFERENCIA);

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.APPROVED);
        verifyNoInteractions(companyBillingEmailQueryPort, paymentGatewayPort, outcomeSettler);
    }

    @Test
    @DisplayName("carrera de reserva perdida: no llama al POST de Wompi, informa PENDING con la referencia de la ganadora (RES2-32)")
    void carrera_de_reserva_perdida_no_llama_a_wompi() {
        when(subscriptionPaymentLedgerPort.findByClientRequestId(EMPRESA, REFERENCIA))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new PaymentReservation(999L, "tx-ganadora")));
        when(companyBillingEmailQueryPort.findFiscalEmail(EMPRESA))
                .thenReturn(Optional.of("facturacion@clinica.co"));
        when(subscriptionPaymentLedgerPort.registerAndApply(eq(EMPRESA),
                eq(new BigDecimal("45000")), eq("COP"), isNull(), any(), eq(REFERENCIA),
                eq(DOCUMENTO))).thenReturn(new PaymentReservationOutcome(999L, true));
        when(subscriptionPaymentLedgerPort.currentOutcome(999L, EMPRESA))
                .thenReturn(Optional.empty());

        GatewayChargeResult result = charger.charge(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO,
                new BigDecimal("45000"), "COP", REFERENCIA);

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.PENDING);
        assertThat(result.gatewayReference()).isEqualTo("tx-ganadora");
        verifyNoInteractions(paymentGatewayPort, outcomeSettler);
    }

    @Test
    @DisplayName("perfil fiscal ausente: propaga sin llamar a la pasarela ni al ledger (UX-05)")
    void perfil_fiscal_ausente_no_llama_a_la_pasarela() {
        when(subscriptionPaymentLedgerPort.findByClientRequestId(EMPRESA, REFERENCIA))
                .thenReturn(Optional.empty());
        when(companyBillingEmailQueryPort.findFiscalEmail(EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> charger.charge(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO,
                new BigDecimal("45000"), "COP", REFERENCIA))
                .isInstanceOf(FiscalProfileNotConfiguredException.class);

        verifyNoInteractions(paymentGatewayPort, outcomeSettler);
    }

    @Test
    @DisplayName("cobra en centavos exactos y confirma cuando el sondeo aprueba")
    void aprobado_en_el_sondeo() {
        conPerfilFiscalYTransaccionPendiente();
        when(paymentGatewayPort.statusPollAttempts()).thenReturn(3);
        when(paymentGatewayPort.statusPollInterval()).thenReturn(Duration.ofMillis(1));
        when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.APPROVED, null, REFERENCIA, 4500000L, "CARD", null));
        when(outcomeSettler.settle(eq(GatewayTransactionStatus.APPROVED), any(), eq(EMPRESA),
                eq(501L), eq(DOCUMENTO), eq(15L), eq(new BigDecimal("45000"))))
                .thenReturn(FirstPeriodChargeOutcome.APPROVED);

        GatewayChargeResult result = charger.charge(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO,
                new BigDecimal("45000"), "COP", REFERENCIA);

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.APPROVED);
        assertThat(result.gatewayReference()).isEqualTo("tx-1");
        assertThat(result.declineReason()).isNull();

        ArgumentCaptor<ChargeRequest> captor = ArgumentCaptor.forClass(ChargeRequest.class);
        verify(paymentGatewayPort).charge(captor.capture());
        assertThat(captor.getValue().amountInCents()).isEqualTo(4500000L);
        assertThat(captor.getValue().paymentSourceId()).isEqualTo(9911L);
        assertThat(captor.getValue().reference()).isEqualTo(REFERENCIA);
        verify(subscriptionPaymentLedgerPort).assignGatewayReference(501L, EMPRESA, "tx-1",
                LocalDateTime.now(RELOJ));
    }

    @Test
    @DisplayName("Wompi trae created_at: se usa como hora real del pago (#783)")
    void usa_el_created_at_de_wompi_para_la_referencia() {
        conPerfilFiscalYTransaccionPendiente();
        Instant creadaEnWompi = Instant.parse("2026-01-01T07:58:00Z");
        when(paymentGatewayPort.charge(any()))
                .thenReturn(new GatewayTransaction("tx-1", GatewayTransactionStatus.PENDING, null,
                        REFERENCIA, 4500000L, "CARD", creadaEnWompi));
        when(paymentGatewayPort.statusPollAttempts()).thenReturn(1);
        when(paymentGatewayPort.statusPollInterval()).thenReturn(Duration.ofMillis(1));
        when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.APPROVED, null, REFERENCIA, 4500000L, "CARD", null));
        when(outcomeSettler.settle(eq(GatewayTransactionStatus.APPROVED), any(), eq(EMPRESA),
                eq(501L), eq(DOCUMENTO), eq(15L), eq(new BigDecimal("45000"))))
                .thenReturn(FirstPeriodChargeOutcome.APPROVED);

        charger.charge(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO, new BigDecimal("45000"), "COP",
                REFERENCIA);

        verify(subscriptionPaymentLedgerPort).assignGatewayReference(501L, EMPRESA, "tx-1",
                LocalDateTime.ofInstant(creadaEnWompi, RELOJ.getZone()));
    }

    @Test
    @DisplayName("reserva antes de llamar a Wompi y asigna la referencia despues (#776)")
    void reserva_antes_de_wompi_y_asigna_despues() {
        conPerfilFiscalYTransaccionPendiente();
        when(paymentGatewayPort.statusPollAttempts()).thenReturn(1);
        when(paymentGatewayPort.statusPollInterval()).thenReturn(Duration.ofMillis(1));
        when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.APPROVED, null, REFERENCIA, 4500000L, "CARD", null));
        when(outcomeSettler.settle(eq(GatewayTransactionStatus.APPROVED), any(), eq(EMPRESA),
                eq(501L), eq(DOCUMENTO), eq(15L), eq(new BigDecimal("45000"))))
                .thenReturn(FirstPeriodChargeOutcome.APPROVED);

        charger.charge(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO, new BigDecimal("45000"), "COP",
                REFERENCIA);

        InOrder order = inOrder(subscriptionPaymentLedgerPort, paymentGatewayPort);
        order.verify(subscriptionPaymentLedgerPort).registerAndApply(eq(EMPRESA),
                eq(new BigDecimal("45000")), eq("COP"), isNull(), any(), eq(REFERENCIA),
                eq(DOCUMENTO));
        order.verify(paymentGatewayPort).charge(any());
        order.verify(subscriptionPaymentLedgerPort).assignGatewayReference(eq(501L), eq(EMPRESA),
                eq("tx-1"), any());
    }

    @Test
    @DisplayName("Wompi lanza tras la reserva: la reserva PENDING queda sin referencia")
    void wompi_lanza_deja_la_reserva_pendiente() {
        when(subscriptionPaymentLedgerPort.findByClientRequestId(EMPRESA, REFERENCIA))
                .thenReturn(Optional.empty());
        when(companyBillingEmailQueryPort.findFiscalEmail(EMPRESA))
                .thenReturn(Optional.of("facturacion@clinica.co"));
        when(subscriptionPaymentLedgerPort.registerAndApply(eq(EMPRESA),
                eq(new BigDecimal("45000")), eq("COP"), isNull(), any(), eq(REFERENCIA),
                eq(DOCUMENTO))).thenReturn(new PaymentReservationOutcome(501L, false));
        when(paymentGatewayPort.charge(any()))
                .thenThrow(new WompiGatewayException("timeout", new RuntimeException("timeout")));

        assertThatThrownBy(() -> charger.charge(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO,
                new BigDecimal("45000"), "COP", REFERENCIA))
                .isInstanceOf(WompiGatewayException.class);

        verify(subscriptionPaymentLedgerPort).registerAndApply(eq(EMPRESA),
                eq(new BigDecimal("45000")), eq("COP"), isNull(), any(), eq(REFERENCIA),
                eq(DOCUMENTO));
        verify(subscriptionPaymentLedgerPort, never()).assignGatewayReference(any(), any(), any(),
                any());
        verifyNoInteractions(outcomeSettler);
    }

    @Test
    @DisplayName("agota el sondeo sin desenlace final: PENDING sin liquidar")
    void pendiente_agotado() {
        conPerfilFiscalYTransaccionPendiente();
        when(paymentGatewayPort.statusPollAttempts()).thenReturn(2);
        when(paymentGatewayPort.statusPollInterval()).thenReturn(Duration.ofMillis(1));
        when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.PENDING, null, REFERENCIA, 4500000L, "CARD", null));

        GatewayChargeResult result = charger.charge(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO,
                new BigDecimal("45000"), "COP", REFERENCIA);

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.PENDING);
        verifyNoInteractions(outcomeSettler);
        verify(metrics).recordChargeOutcome(FirstPeriodChargeOutcome.PENDING, null);
    }

    @Test
    @DisplayName("declinado: propaga el motivo crudo del settler")
    void declinado_propaga_el_motivo() {
        conPerfilFiscalYTransaccionPendiente();
        when(paymentGatewayPort.statusPollAttempts()).thenReturn(1);
        when(paymentGatewayPort.statusPollInterval()).thenReturn(Duration.ofMillis(1));
        when(paymentGatewayPort.findTransaction("tx-1"))
                .thenReturn(new GatewayTransaction("tx-1", GatewayTransactionStatus.DECLINED,
                        "Fondos insuficientes", REFERENCIA, 4500000L, "CARD", null));
        when(outcomeSettler.settle(eq(GatewayTransactionStatus.DECLINED),
                eq("Fondos insuficientes"), eq(EMPRESA), eq(501L), eq(DOCUMENTO), eq(15L),
                eq(new BigDecimal("45000")))).thenReturn(FirstPeriodChargeOutcome.DECLINED);

        GatewayChargeResult result = charger.charge(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO,
                new BigDecimal("45000"), "COP", REFERENCIA);

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.DECLINED);
        assertThat(result.declineReason()).isEqualTo("Fondos insuficientes");
    }
}
