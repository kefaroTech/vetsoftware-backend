package com.vetsoftware.app.paymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.command.ChargeBillingDocumentCommand;
import com.vetsoftware.app.paymentgateway.application.dto.DocumentChargeDto;
import com.vetsoftware.app.paymentgateway.application.dto.GatewayChargeResult;
import com.vetsoftware.app.paymentgateway.application.port.out.BillingDocumentChargeQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PendingPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.domain.BillingDocumentChargeSnapshot;
import com.vetsoftware.app.paymentgateway.domain.DocumentChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.LastPaymentAttempt;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNotConfiguredException;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
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
@DisplayName("ChargeBillingDocumentService")
class ChargeBillingDocumentServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long DOCUMENTO = 900L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-04T08:15:30Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.ofInstant(RELOJ.instant(),
            RELOJ.getZone());

    @Mock
    private BillingDocumentChargeQueryPort documentQueryPort;
    @Mock
    private PendingPaymentQueryPort pendingPaymentQueryPort;
    @Mock
    private PaymentAttemptQueryPort paymentAttemptQueryPort;
    @Mock
    private DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort;
    @Mock
    private PaymentAttemptRecorderPort paymentAttemptRecorderPort;
    @Mock
    private GatewayCharger gatewayCharger;

    private ChargeBillingDocumentService service;

    @BeforeEach
    void setUp() {
        service = new ChargeBillingDocumentService(documentQueryPort, pendingPaymentQueryPort,
                paymentAttemptQueryPort, defaultCardPaymentMethodQueryPort,
                paymentAttemptRecorderPort, gatewayCharger, RELOJ);
    }

    private ChargeBillingDocumentCommand comando() {
        return new ChargeBillingDocumentCommand(EMPRESA, DOCUMENTO);
    }

    private void documento(BigDecimal total, BigDecimal saldo) {
        when(documentQueryPort.findByIdAndCompanyId(DOCUMENTO, EMPRESA)).thenReturn(Optional
                .of(new BillingDocumentChargeSnapshot(DOCUMENTO, "FV-1", total, saldo, "COP", 7L)));
    }

    @Test
    @DisplayName("sin saldo: SKIPPED_NO_BALANCE sin tocar nada mas")
    void sin_saldo() {
        documento(new BigDecimal("45000"), BigDecimal.ZERO);

        DocumentChargeDto result = service.execute(comando());

        assertThat(result.outcome()).isEqualTo(DocumentChargeOutcome.SKIPPED_NO_BALANCE);
        verifyNoInteractions(pendingPaymentQueryPort, paymentAttemptQueryPort, gatewayCharger);
    }

    @Test
    @DisplayName("con un pago PENDING ya aplicado: SKIPPED_PENDING_PAYMENT")
    void con_pago_pendiente_aplicado() {
        documento(new BigDecimal("45000"), new BigDecimal("45000"));
        when(pendingPaymentQueryPort.existsPendingPayment(EMPRESA, DOCUMENTO)).thenReturn(true);

        DocumentChargeDto result = service.execute(comando());

        assertThat(result.outcome()).isEqualTo(DocumentChargeOutcome.SKIPPED_PENDING_PAYMENT);
        verifyNoInteractions(paymentAttemptQueryPort, gatewayCharger);
    }

    @Nested
    @DisplayName("con saldo y sin pago pendiente")
    class ConSaldo {

        @BeforeEach
        void base() {
            documento(new BigDecimal("45000"), new BigDecimal("30000"));
            when(pendingPaymentQueryPort.existsPendingPayment(EMPRESA, DOCUMENTO))
                    .thenReturn(false);
        }

        @Test
        @DisplayName("ultimo intento HARD: SKIPPED_HARD_DECLINE")
        void ultimo_intento_hard() {
            when(paymentAttemptQueryPort.findLast(EMPRESA, DOCUMENTO)).thenReturn(Optional.of(
                    new LastPaymentAttempt(1, GatewayDeclineKind.HARD, AHORA.minusDays(1), null)));

            DocumentChargeDto result = service.execute(comando());

            assertThat(result.outcome()).isEqualTo(DocumentChargeOutcome.SKIPPED_HARD_DECLINE);
            verifyNoInteractions(gatewayCharger);
        }

        @Test
        @DisplayName("ultimo intento SOFT con nextAttemptAt futuro: SKIPPED_NOT_DUE")
        void ultimo_intento_soft_no_vencido() {
            when(paymentAttemptQueryPort.findLast(EMPRESA, DOCUMENTO))
                    .thenReturn(Optional.of(new LastPaymentAttempt(1, GatewayDeclineKind.SOFT,
                            AHORA.minusHours(1), AHORA.plusDays(1))));

            DocumentChargeDto result = service.execute(comando());

            assertThat(result.outcome()).isEqualTo(DocumentChargeOutcome.SKIPPED_NOT_DUE);
            verifyNoInteractions(gatewayCharger);
        }

        @Test
        @DisplayName("presupuesto de reintentos agotado (4 en 14 dias): SKIPPED_BUDGET, nunca 409")
        void presupuesto_agotado() {
            when(paymentAttemptQueryPort.findLast(EMPRESA, DOCUMENTO))
                    .thenReturn(Optional.of(new LastPaymentAttempt(3, GatewayDeclineKind.SOFT,
                            AHORA.minusDays(1), AHORA.minusHours(1))));
            when(paymentAttemptQueryPort.countRetryableSince(eq(EMPRESA), eq(DOCUMENTO), any()))
                    .thenReturn(4);

            DocumentChargeDto result = service.execute(comando());

            assertThat(result.outcome()).isEqualTo(DocumentChargeOutcome.SKIPPED_BUDGET);
            verifyNoInteractions(gatewayCharger);
        }

        @Nested
        @DisplayName("bajo el presupuesto")
        class BajoElPresupuesto {

            @BeforeEach
            void presupuestoDisponible() {
                when(paymentAttemptQueryPort.countRetryableSince(eq(EMPRESA), eq(DOCUMENTO), any()))
                        .thenReturn(0);
            }

            @Test
            @DisplayName("sin medio de pago y sin intento CONFIGURATION previo: anota uno nuevo")
            void sin_medio_de_pago_sin_intento_previo() {
                when(paymentAttemptQueryPort.findLast(EMPRESA, DOCUMENTO))
                        .thenReturn(Optional.empty());
                when(defaultCardPaymentMethodQueryPort.findDefaultActiveCard(EMPRESA, "WOMPI"))
                        .thenReturn(Optional.empty());

                DocumentChargeDto result = service.execute(comando());

                assertThat(result.outcome()).isEqualTo(DocumentChargeOutcome.NO_PAYMENT_METHOD);
                verify(paymentAttemptRecorderPort).record(eq(EMPRESA), eq(DOCUMENTO), isNull(),
                        eq("WOMPI"), eq(new BigDecimal("30000")), isNull(),
                        eq(GatewayDeclineKind.CONFIGURATION), eq(AHORA), isNull());
                verifyNoInteractions(gatewayCharger);
            }

            @Test
            @DisplayName("sin medio de pago con un CONFIGURATION de hace 2 horas: no repite la fila")
            void sin_medio_de_pago_con_intento_configuration_reciente() {
                when(paymentAttemptQueryPort.findLast(EMPRESA, DOCUMENTO))
                        .thenReturn(Optional.of(new LastPaymentAttempt(1,
                                GatewayDeclineKind.CONFIGURATION, AHORA.minusHours(2), null)));
                when(defaultCardPaymentMethodQueryPort.findDefaultActiveCard(EMPRESA, "WOMPI"))
                        .thenReturn(Optional.empty());

                DocumentChargeDto result = service.execute(comando());

                assertThat(result.outcome()).isEqualTo(DocumentChargeOutcome.NO_PAYMENT_METHOD);
                verify(paymentAttemptRecorderPort, never()).record(any(), any(), any(), any(),
                        any(), any(), any(), any(), any());
            }

            @Test
            @DisplayName("sin medio de pago con un CONFIGURATION de hace 30 horas: anota uno nuevo")
            void sin_medio_de_pago_con_intento_configuration_viejo() {
                when(paymentAttemptQueryPort.findLast(EMPRESA, DOCUMENTO))
                        .thenReturn(Optional.of(new LastPaymentAttempt(1,
                                GatewayDeclineKind.CONFIGURATION, AHORA.minusHours(30), null)));
                when(defaultCardPaymentMethodQueryPort.findDefaultActiveCard(EMPRESA, "WOMPI"))
                        .thenReturn(Optional.empty());

                service.execute(comando());

                verify(paymentAttemptRecorderPort).record(eq(EMPRESA), eq(DOCUMENTO), isNull(),
                        eq("WOMPI"), eq(new BigDecimal("30000")), isNull(),
                        eq(GatewayDeclineKind.CONFIGURATION), eq(AHORA), isNull());
            }

            @Nested
            @DisplayName("con medio de pago activo")
            class ConMedioDePago {

                private final PaymentMethodRef medio = new PaymentMethodRef(15L, "9911");

                @BeforeEach
                void medioDePago() {
                    when(paymentAttemptQueryPort.findLast(EMPRESA, DOCUMENTO))
                            .thenReturn(Optional.empty());
                    when(defaultCardPaymentMethodQueryPort.findDefaultActiveCard(EMPRESA, "WOMPI"))
                            .thenReturn(Optional.of(medio));
                }

                @Test
                @DisplayName("aprobado: cobra exactamente el saldo, no el total, como VS-DOC-<id>-A1")
                void aprobado_por_el_saldo() {
                    when(gatewayCharger.charge(EMPRESA, DOCUMENTO, medio, new BigDecimal("30000"),
                            "COP", "VS-DOC-900-A1"))
                            .thenReturn(new GatewayChargeResult(FirstPeriodChargeOutcome.APPROVED,
                                    "tx-1", null));

                    DocumentChargeDto result = service.execute(comando());

                    assertThat(result.outcome()).isEqualTo(DocumentChargeOutcome.APPROVED);
                    assertThat(result.gatewayReference()).isEqualTo("tx-1");
                }

                @Test
                @DisplayName("segundo intento del documento: la referencia lleva A2, no A1")
                void segundo_intento_incrementa_el_consecutivo() {
                    when(paymentAttemptQueryPort.findLast(EMPRESA, DOCUMENTO)).thenReturn(
                            Optional.of(new LastPaymentAttempt(1, GatewayDeclineKind.SOFT,
                                    AHORA.minusDays(1), AHORA.minusHours(1))));
                    when(gatewayCharger.charge(EMPRESA, DOCUMENTO, medio, new BigDecimal("30000"),
                            "COP", "VS-DOC-900-A2"))
                            .thenReturn(new GatewayChargeResult(FirstPeriodChargeOutcome.PENDING,
                                    "tx-2", null));

                    DocumentChargeDto result = service.execute(comando());

                    assertThat(result.outcome()).isEqualTo(DocumentChargeOutcome.PENDING);
                    verify(gatewayCharger).charge(EMPRESA, DOCUMENTO, medio,
                            new BigDecimal("30000"), "COP", "VS-DOC-900-A2");
                }

                @Test
                @DisplayName("pendiente: agotado el sondeo, sin liquidar")
                void pendiente_agotado() {
                    when(gatewayCharger.charge(any(), any(), any(), any(), any(), any()))
                            .thenReturn(new GatewayChargeResult(FirstPeriodChargeOutcome.PENDING,
                                    "tx-1", null));

                    DocumentChargeDto result = service.execute(comando());

                    assertThat(result.outcome()).isEqualTo(DocumentChargeOutcome.PENDING);
                }

                @Test
                @DisplayName("declinado SOFT: propaga el motivo, la fecha la puso el settler")
                void declinado_soft() {
                    when(gatewayCharger.charge(any(), any(), any(), any(), any(), any()))
                            .thenReturn(new GatewayChargeResult(FirstPeriodChargeOutcome.DECLINED,
                                    "tx-1", "Fondos insuficientes"));

                    DocumentChargeDto result = service.execute(comando());

                    assertThat(result.outcome()).isEqualTo(DocumentChargeOutcome.DECLINED);
                    assertThat(result.declineReason()).isEqualTo("Fondos insuficientes");
                }

                @Test
                @DisplayName("pasarela deshabilitada durante el cobro: NOT_CONFIGURED, nunca 500")
                void pasarela_deshabilitada() {
                    when(gatewayCharger.charge(any(), any(), any(), any(), any(), any())).thenThrow(
                            new PaymentGatewayNotConfiguredException("WOMPI_ENABLED=false"));

                    DocumentChargeDto result = service.execute(comando());

                    assertThat(result.outcome()).isEqualTo(DocumentChargeOutcome.NOT_CONFIGURED);
                }
            }
        }
    }
}
