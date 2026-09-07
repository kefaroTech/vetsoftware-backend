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
import com.vetsoftware.app.paymentgateway.application.dto.GatewayChargeResult;
import com.vetsoftware.app.paymentgateway.application.port.out.BillingDocumentIssuerPort;
import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.FirstPeriodPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import com.vetsoftware.app.paymentgateway.domain.FiscalProfileNotConfiguredException;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.IssuedPeriodDocument;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import io.micrometer.observation.ObservationRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private PaymentAttemptRecorderPort paymentAttemptRecorderPort;
    @Mock
    private GatewayCharger gatewayCharger;

    private ChargeContractFirstPeriodService service;

    @BeforeEach
    void setUp() {
        service = new ChargeContractFirstPeriodService(firstPeriodPaymentQueryPort,
                billingDocumentIssuerPort, defaultCardPaymentMethodQueryPort,
                paymentAttemptRecorderPort, gatewayCharger, ObservationRegistry.NOOP, RELOJ);
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
        verifyNoInteractions(billingDocumentIssuerPort, gatewayCharger);
    }

    @Test
    @DisplayName("idempotencia: un pago REFUNDED no se muestra como aprobado (RES-60)")
    void idempotencia_pago_refunded_no_es_aprobado() {
        when(firstPeriodPaymentQueryPort.findByCompanyIdAndReference(EMPRESA, REFERENCIA))
                .thenReturn(Optional.of(new FirstPeriodPaymentSnapshot(501L, EMPRESA, "REFUNDED",
                        new BigDecimal("45000"), "COP", "tx-1", null)));

        FirstPeriodChargeDto result = service.execute(comando());

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.DECLINED);
        verifyNoInteractions(billingDocumentIssuerPort, gatewayCharger);
    }

    @Test
    @DisplayName("sin nada que facturar: NOT_CONFIGURED sin tocar la pasarela")
    void sin_nada_que_facturar() {
        when(firstPeriodPaymentQueryPort.findByCompanyIdAndReference(EMPRESA, REFERENCIA))
                .thenReturn(Optional.empty());
        when(billingDocumentIssuerPort.issue(EMPRESA, CONTRATO, INICIO, FIN))
                .thenReturn(new IssuedPeriodDocument(null, null, null, null, null, false));

        FirstPeriodChargeDto result = service.execute(comando());

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.NOT_CONFIGURED);
        verifyNoInteractions(gatewayCharger);
    }

    @Test
    @DisplayName("saldo cero (cubierto por abono): APPROVED sin llamar a la pasarela (RES2-09)")
    void saldo_cero_no_llama_a_la_pasarela() {
        when(firstPeriodPaymentQueryPort.findByCompanyIdAndReference(EMPRESA, REFERENCIA))
                .thenReturn(Optional.empty());
        when(billingDocumentIssuerPort.issue(EMPRESA, CONTRATO, INICIO, FIN))
                .thenReturn(new IssuedPeriodDocument(900L, "FV-1", new BigDecimal("45000"),
                        BigDecimal.ZERO, "COP", true));

        FirstPeriodChargeDto result = service.execute(comando());

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.APPROVED);
        assertThat(result.gatewayReference()).isNull();
        verifyNoInteractions(gatewayCharger, defaultCardPaymentMethodQueryPort,
                paymentAttemptRecorderPort);
    }

    @Test
    @DisplayName("sin medio de pago: NO_PAYMENT_METHOD y anota un intento CONFIGURATION reprogramado a +1 dia")
    void sin_medio_de_pago() {
        when(firstPeriodPaymentQueryPort.findByCompanyIdAndReference(EMPRESA, REFERENCIA))
                .thenReturn(Optional.empty());
        when(billingDocumentIssuerPort.issue(EMPRESA, CONTRATO, INICIO, FIN))
                .thenReturn(new IssuedPeriodDocument(900L, "FV-1", new BigDecimal("45000"),
                        new BigDecimal("45000"), "COP", true));
        when(defaultCardPaymentMethodQueryPort.findDefaultActiveCard(eq(EMPRESA), eq("WOMPI")))
                .thenReturn(Optional.empty());

        FirstPeriodChargeDto result = service.execute(comando());

        assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.NO_PAYMENT_METHOD);
        ArgumentCaptor<java.time.LocalDateTime> nextAttemptCaptor = ArgumentCaptor
                .forClass(java.time.LocalDateTime.class);
        verify(paymentAttemptRecorderPort).record(eq(EMPRESA), eq(900L), isNull(), eq("WOMPI"),
                eq(new BigDecimal("45000")), isNull(), eq(GatewayDeclineKind.CONFIGURATION), any(),
                nextAttemptCaptor.capture());
        assertThat(nextAttemptCaptor.getValue())
                .isEqualTo(java.time.LocalDateTime.now(RELOJ).plusDays(1));
        verifyNoInteractions(gatewayCharger);
    }

    @Nested
    @DisplayName("con medio de pago activo")
    class ConMedioDePago {

        @BeforeEach
        void conCobroPosible() {
            when(firstPeriodPaymentQueryPort.findByCompanyIdAndReference(EMPRESA, REFERENCIA))
                    .thenReturn(Optional.empty());
            when(billingDocumentIssuerPort.issue(EMPRESA, CONTRATO, INICIO, FIN))
                    .thenReturn(new IssuedPeriodDocument(900L, "FV-1", new BigDecimal("45000"),
                            new BigDecimal("45000"), "COP", true));
            when(defaultCardPaymentMethodQueryPort.findDefaultActiveCard(eq(EMPRESA), eq("WOMPI")))
                    .thenReturn(Optional.of(new PaymentMethodRef(15L, "9911")));
        }

        @Test
        @DisplayName("aprobado: delega en GatewayCharger con el total del documento")
        void aprobado() {
            when(gatewayCharger.charge(eq(EMPRESA), eq(900L), eq(new PaymentMethodRef(15L, "9911")),
                    eq(new BigDecimal("45000")), eq("COP"), eq(REFERENCIA)))
                    .thenReturn(new GatewayChargeResult(FirstPeriodChargeOutcome.APPROVED, "tx-1",
                            null));

            FirstPeriodChargeDto result = service.execute(comando());

            assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.APPROVED);
            assertThat(result.gatewayReference()).isEqualTo("tx-1");

            ArgumentCaptor<BigDecimal> montoCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            verify(gatewayCharger).charge(eq(EMPRESA), eq(900L), any(PaymentMethodRef.class),
                    montoCaptor.capture(), eq("COP"), eq(REFERENCIA));
            assertThat(montoCaptor.getValue()).isEqualByComparingTo("45000");
        }

        @Test
        @DisplayName("con un abono parcial ya aplicado: cobra el balanceAmount, no el totalAmount (RES2-09)")
        void cobra_el_saldo_no_el_total() {
            when(billingDocumentIssuerPort.issue(EMPRESA, CONTRATO, INICIO, FIN))
                    .thenReturn(new IssuedPeriodDocument(900L, "FV-1", new BigDecimal("45000"),
                            new BigDecimal("10000"), "COP", true));
            when(gatewayCharger.charge(eq(EMPRESA), eq(900L), any(PaymentMethodRef.class),
                    eq(new BigDecimal("10000")), eq("COP"), eq(REFERENCIA)))
                    .thenReturn(new GatewayChargeResult(FirstPeriodChargeOutcome.APPROVED, "tx-1",
                            null));

            service.execute(comando());

            verify(gatewayCharger).charge(eq(EMPRESA), eq(900L), any(PaymentMethodRef.class),
                    eq(new BigDecimal("10000")), eq("COP"), eq(REFERENCIA));
        }

        @Test
        @DisplayName("pendiente: traslada el desenlace de GatewayCharger tal cual")
        void pendiente() {
            when(gatewayCharger.charge(any(), any(), any(), any(), any(), any())).thenReturn(
                    new GatewayChargeResult(FirstPeriodChargeOutcome.PENDING, "tx-1", null));

            FirstPeriodChargeDto result = service.execute(comando());

            assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.PENDING);
        }

        @Test
        @DisplayName("rechazado: propaga el motivo del rechazo")
        void rechazado() {
            when(gatewayCharger.charge(any(), any(), any(), any(), any(), any()))
                    .thenReturn(new GatewayChargeResult(FirstPeriodChargeOutcome.DECLINED, "tx-1",
                            "Fondos insuficientes"));

            FirstPeriodChargeDto result = service.execute(comando());

            assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.DECLINED);
            assertThat(result.declineReason()).isEqualTo("Fondos insuficientes");
        }

        @Test
        @DisplayName("rechazado: el documento del periodo, no el contrato, es el que entra a la cola de reintentos")
        void rechazado_pasa_el_documento_del_periodo_al_charger() {
            when(gatewayCharger.charge(any(), any(), any(), any(), any(), any()))
                    .thenReturn(new GatewayChargeResult(FirstPeriodChargeOutcome.DECLINED, "tx-1",
                            "Fondos insuficientes"));

            service.execute(comando());

            verify(gatewayCharger).charge(eq(EMPRESA), eq(900L), any(PaymentMethodRef.class), any(),
                    eq("COP"), eq(REFERENCIA));
        }

        @Test
        @DisplayName("perfil fiscal ausente: NOT_CONFIGURED y anota un intento CONFIGURATION reprogramado a +1 dia (UX-05)")
        void perfil_fiscal_ausente() {
            when(gatewayCharger.charge(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new FiscalProfileNotConfiguredException(EMPRESA));

            FirstPeriodChargeDto result = service.execute(comando());

            assertThat(result.outcome()).isEqualTo(FirstPeriodChargeOutcome.NOT_CONFIGURED);
            ArgumentCaptor<java.time.LocalDateTime> nextAttemptCaptor = ArgumentCaptor
                    .forClass(java.time.LocalDateTime.class);
            verify(paymentAttemptRecorderPort).record(eq(EMPRESA), eq(900L), isNull(), eq("WOMPI"),
                    eq(new BigDecimal("45000")), isNull(), eq(GatewayDeclineKind.CONFIGURATION),
                    any(), nextAttemptCaptor.capture());
            assertThat(nextAttemptCaptor.getValue())
                    .isEqualTo(java.time.LocalDateTime.now(RELOJ).plusDays(1));
        }
    }
}
