package com.vetsoftware.app.paymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.dto.GatewayChargeResult;
import com.vetsoftware.app.paymentgateway.application.port.out.CompanyBillingEmailQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayPort;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.domain.ChargeRequest;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransaction;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    private GatewayCharger charger;

    @BeforeEach
    void setUp() {
        charger = new GatewayCharger(companyBillingEmailQueryPort, paymentGatewayPort,
                subscriptionPaymentLedgerPort, outcomeSettler, RELOJ);
        when(companyBillingEmailQueryPort.findFiscalEmail(EMPRESA))
                .thenReturn(Optional.of("facturacion@clinica.co"));
        when(paymentGatewayPort.charge(any())).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.PENDING, null, REFERENCIA, 4500000L, "CARD"));
        when(subscriptionPaymentLedgerPort.registerAndApply(eq(EMPRESA),
                eq(new BigDecimal("45000")), eq("COP"), eq("tx-1"), any(), eq(REFERENCIA),
                eq(DOCUMENTO))).thenReturn(501L);
    }

    @Test
    @DisplayName("cobra en centavos exactos y confirma cuando el sondeo aprueba")
    void aprobado_en_el_sondeo() {
        when(paymentGatewayPort.statusPollAttempts()).thenReturn(3);
        when(paymentGatewayPort.statusPollInterval()).thenReturn(Duration.ofMillis(1));
        when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.APPROVED, null, REFERENCIA, 4500000L, "CARD"));
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
    }

    @Test
    @DisplayName("agota el sondeo sin desenlace final: PENDING sin liquidar")
    void pendiente_agotado() {
        when(paymentGatewayPort.statusPollAttempts()).thenReturn(2);
        when(paymentGatewayPort.statusPollInterval()).thenReturn(Duration.ofMillis(1));
        when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction("tx-1",
                GatewayTransactionStatus.PENDING, null, REFERENCIA, 4500000L, "CARD"));

        GatewayChargeResult result = charger.charge(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO,
                new BigDecimal("45000"), "COP", REFERENCIA);

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.PENDING);
        verifyNoInteractions(outcomeSettler);
    }

    @Test
    @DisplayName("declinado: propaga el motivo crudo del settler")
    void declinado_propaga_el_motivo() {
        when(paymentGatewayPort.statusPollAttempts()).thenReturn(1);
        when(paymentGatewayPort.statusPollInterval()).thenReturn(Duration.ofMillis(1));
        when(paymentGatewayPort.findTransaction("tx-1"))
                .thenReturn(new GatewayTransaction("tx-1", GatewayTransactionStatus.DECLINED,
                        "Fondos insuficientes", REFERENCIA, 4500000L, "CARD"));
        when(outcomeSettler.settle(eq(GatewayTransactionStatus.DECLINED),
                eq("Fondos insuficientes"), eq(EMPRESA), eq(501L), eq(DOCUMENTO), eq(15L),
                eq(new BigDecimal("45000")))).thenReturn(FirstPeriodChargeOutcome.DECLINED);

        GatewayChargeResult result = charger.charge(EMPRESA, DOCUMENTO, MEDIO_DE_PAGO,
                new BigDecimal("45000"), "COP", REFERENCIA);

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.DECLINED);
        assertThat(result.declineReason()).isEqualTo("Fondos insuficientes");
    }
}
