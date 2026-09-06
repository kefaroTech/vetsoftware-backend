package com.vetsoftware.app.paymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.command.ChargeContractFirstPeriodCommand;
import com.vetsoftware.app.paymentgateway.application.dto.FirstPeriodChargeDto;
import com.vetsoftware.app.paymentgateway.application.port.out.BillingDocumentIssuerPort;
import com.vetsoftware.app.paymentgateway.application.port.out.CompanyBillingEmailQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.FirstPeriodPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayPort;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.domain.ChargeRequest;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransaction;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.IssuedPeriodDocument;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChargeContractFirstPeriodService")
class ChargeContractFirstPeriodServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final String NUMERO = "SUS-2026-00184";
    private static final String REFERENCIA = "VS-SUS-2026-00184-P1";
    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 1, 31);
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-01-01T08:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort;
    @Mock
    private BillingDocumentIssuerPort billingDocumentIssuerPort;
    @Mock
    private DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort;
    @Mock
    private CompanyBillingEmailQueryPort companyBillingEmailQueryPort;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort;
    @Mock
    private PaymentAttemptRecorderPort paymentAttemptRecorderPort;
    @Mock
    private GatewayOutcomeSettler outcomeSettler;

    private ChargeContractFirstPeriodService service;

    @BeforeEach
    void setUp() {
        service = new ChargeContractFirstPeriodService(firstPeriodPaymentQueryPort,
                billingDocumentIssuerPort, defaultCardPaymentMethodQueryPort,
                companyBillingEmailQueryPort, paymentGatewayPort, subscriptionPaymentLedgerPort,
                paymentAttemptRecorderPort, outcomeSettler, RELOJ);
    }

    private ChargeContractFirstPeriodCommand comando() {
        return new ChargeContractFirstPeriodCommand(EMPRESA, CONTRATO, NUMERO, INICIO, FIN);
    }

    @Test
    @DisplayName("idempotencia: un pago ya registrado con esa referencia no vuelve a cobrar")
    void idempotencia_por_referencia() {
        when(firstPeriodPaymentQueryPort.findByCompanyIdAndReference(EMPRESA, REFERENCIA))
                .thenReturn(Optional.of(new FirstPeriodPaymentSnapshot(501L, EMPRESA, "CONFIRMED",
                        new BigDecimal("45000"), "COP", "tx-1", null)));

        FirstPeriodChargeDto result = service.execute(comando());

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.APPROVED);
        assertThat(result.gatewayReference()).isEqualTo("tx-1");
        verifyNoInteractions(billingDocumentIssuerPort, paymentGatewayPort);
    }

    @Test
    @DisplayName("sin nada que facturar: NOT_CONFIGURED sin tocar la pasarela")
    void sin_nada_que_facturar() {
        when(firstPeriodPaymentQueryPort.findByCompanyIdAndReference(EMPRESA, REFERENCIA))
                .thenReturn(Optional.empty());
        when(billingDocumentIssuerPort.issue(EMPRESA, CONTRATO, INICIO, FIN))
                .thenReturn(new IssuedPeriodDocument(null, null, null, null, false));

        FirstPeriodChargeDto result = service.execute(comando());

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.NOT_CONFIGURED);
        verifyNoInteractions(paymentGatewayPort);
    }

    @Test
    @DisplayName("sin medio de pago: NO_PAYMENT_METHOD y anota un intento CONFIGURATION")
    void sin_medio_de_pago() {
        when(firstPeriodPaymentQueryPort.findByCompanyIdAndReference(EMPRESA, REFERENCIA))
                .thenReturn(Optional.empty());
        when(billingDocumentIssuerPort.issue(EMPRESA, CONTRATO, INICIO, FIN)).thenReturn(
                new IssuedPeriodDocument(900L, "FV-1", new BigDecimal("45000"), "COP", true));
        when(defaultCardPaymentMethodQueryPort.findDefaultActiveCard(eq(EMPRESA), eq("WOMPI")))
                .thenReturn(Optional.empty());

        FirstPeriodChargeDto result = service.execute(comando());

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.NO_PAYMENT_METHOD);
        verify(paymentAttemptRecorderPort).record(eq(EMPRESA), eq(900L), isNull(), eq("WOMPI"),
                eq(new BigDecimal("45000")), isNull(), eq(GatewayDeclineKind.CONFIGURATION), any(),
                isNull());
        verifyNoInteractions(paymentGatewayPort);
    }

    @Nested
    @DisplayName("con medio de pago activo")
    class ConMedioDePago {

        @BeforeEach
        void conCobroPosible() {
            when(firstPeriodPaymentQueryPort.findByCompanyIdAndReference(EMPRESA, REFERENCIA))
                    .thenReturn(Optional.empty());
            when(billingDocumentIssuerPort.issue(EMPRESA, CONTRATO, INICIO, FIN)).thenReturn(
                    new IssuedPeriodDocument(900L, "FV-1", new BigDecimal("45000"), "COP", true));
            when(defaultCardPaymentMethodQueryPort.findDefaultActiveCard(eq(EMPRESA), eq("WOMPI")))
                    .thenReturn(Optional.of(new PaymentMethodRef(15L, "9911")));
            when(companyBillingEmailQueryPort.findFiscalEmail(EMPRESA))
                    .thenReturn(Optional.of("facturacion@clinica.co"));
            when(paymentGatewayPort.charge(any())).thenReturn(new GatewayTransaction("tx-1",
                    GatewayTransactionStatus.PENDING, null, REFERENCIA, 4500000L, "CARD"));
            when(subscriptionPaymentLedgerPort.registerAndApply(eq(EMPRESA),
                    eq(new BigDecimal("45000")), eq("COP"), eq("tx-1"), any(), eq(REFERENCIA),
                    eq(900L))).thenReturn(501L);
        }

        @Test
        @DisplayName("aprobado en el sondeo: cobra en centavos exactos y confirma")
        void aprobado_en_el_sondeo() {
            when(paymentGatewayPort.statusPollAttempts()).thenReturn(3);
            when(paymentGatewayPort.statusPollInterval()).thenReturn(Duration.ofMillis(1));
            when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction(
                    "tx-1", GatewayTransactionStatus.APPROVED, null, REFERENCIA, 4500000L, "CARD"));
            when(outcomeSettler.settle(eq(GatewayTransactionStatus.APPROVED), any(), eq(EMPRESA),
                    eq(501L), eq(900L), eq(15L), eq(new BigDecimal("45000"))))
                    .thenReturn(FirstPeriodChargeOutcome.APPROVED);

            FirstPeriodChargeDto result = service.execute(comando());

            assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.APPROVED);
            org.mockito.ArgumentCaptor<ChargeRequest> captor = org.mockito.ArgumentCaptor
                    .forClass(ChargeRequest.class);
            verify(paymentGatewayPort).charge(captor.capture());
            assertThat(captor.getValue().amountInCents()).isEqualTo(4500000L);
            assertThat(captor.getValue().paymentSourceId()).isEqualTo(9911L);
            assertThat(captor.getValue().reference()).isEqualTo(REFERENCIA);
        }

        @Test
        @DisplayName("agota el sondeo sin desenlace final: se queda en PENDING sin liquidar")
        void pendiente_agotado() {
            when(paymentGatewayPort.statusPollAttempts()).thenReturn(2);
            when(paymentGatewayPort.statusPollInterval()).thenReturn(Duration.ofMillis(1));
            when(paymentGatewayPort.findTransaction("tx-1")).thenReturn(new GatewayTransaction(
                    "tx-1", GatewayTransactionStatus.PENDING, null, REFERENCIA, 4500000L, "CARD"));

            FirstPeriodChargeDto result = service.execute(comando());

            assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.PENDING);
            verifyNoInteractions(outcomeSettler);
        }
    }
}
