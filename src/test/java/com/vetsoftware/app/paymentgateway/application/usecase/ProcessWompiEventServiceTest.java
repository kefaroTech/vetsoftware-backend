package com.vetsoftware.app.paymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import com.vetsoftware.app.paymentgateway.application.command.ProcessWompiEventCommand;
import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.FirstPeriodPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.GatewayWebhookEventRecorderPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.application.port.out.WompiEventPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.GatewayWebhookOutcome;
import com.vetsoftware.app.paymentgateway.domain.ParsedWompiEvent;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNotConfiguredException;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import com.vetsoftware.app.paymentgateway.domain.WompiChecksumMismatchException;
import com.vetsoftware.app.paymentgateway.domain.WompiMalformedEventException;
import com.vetsoftware.app.paymentgateway.domain.WompiStaleEventException;
import io.micrometer.observation.ObservationRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessWompiEventService")
class ProcessWompiEventServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long PAGO = 501L;
    private static final String RAW_BODY = "{}";
    private static final String CHECKSUM = "abc123";
    private static final long TIMESTAMP = 1530291411L;
    private static final long MONTO_REGISTRADO_EN_CENTAVOS = 4_500_000L;

    @Mock
    private WompiEventPort wompiEventPort;
    @Mock
    private FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort;
    @Mock
    private DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort;
    @Mock
    private SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort;
    @Mock
    private GatewayOutcomeSettler outcomeSettler;
    @Mock
    private GatewayWebhookEventRecorderPort webhookEventRecorderPort;
    @Mock
    private PaymentGatewayMetrics metrics;

    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(TIMESTAMP), ZoneOffset.UTC);

    private ProcessWompiEventService service;

    @BeforeEach
    void setUp() {
        service = new ProcessWompiEventService(wompiEventPort, firstPeriodPaymentQueryPort,
                defaultCardPaymentMethodQueryPort, subscriptionPaymentLedgerPort, outcomeSettler,
                webhookEventRecorderPort, metrics, ObservationRegistry.NOOP, clock);
    }

    private ParsedWompiEvent evento(String tipo, GatewayTransactionStatus estado, String mensaje,
            long timestamp, long amountInCents) {
        return new ParsedWompiEvent(tipo, "tx-1", estado, mensaje, timestamp, amountInCents,
                List.of("a", "b"));
    }

    private ParsedWompiEvent evento(String tipo, GatewayTransactionStatus estado, String mensaje) {
        return evento(tipo, estado, mensaje, TIMESTAMP, MONTO_REGISTRADO_EN_CENTAVOS);
    }

    private void stubAutenticado(ParsedWompiEvent evento) {
        when(wompiEventPort.parse(RAW_BODY)).thenReturn(evento);
        when(wompiEventPort.matchesChecksum(evento, CHECKSUM)).thenReturn(true);
        when(wompiEventPort.freshnessTolerance()).thenReturn(Duration.ofHours(24));
    }

    @Test
    @DisplayName("Wompi deshabilitado o sin secreto de eventos propaga sin tocar nada mas")
    void gateway_no_configurado_propaga() {
        ParsedWompiEvent evento = evento("transaction.updated", GatewayTransactionStatus.APPROVED,
                null);
        when(wompiEventPort.parse(RAW_BODY)).thenReturn(evento);
        doThrow(new PaymentGatewayNotConfiguredException("Wompi no esta habilitado"))
                .when(wompiEventPort).requireConfigured();

        assertThatThrownBy(() -> service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM)))
                .isInstanceOf(PaymentGatewayNotConfiguredException.class);
        verifyNoInteractions(webhookEventRecorderPort, firstPeriodPaymentQueryPort, outcomeSettler);
    }

    @Test
    @DisplayName("un cuerpo malformado propaga sin persistir nada")
    void cuerpo_malformado_propaga() {
        when(wompiEventPort.parse(RAW_BODY))
                .thenThrow(new WompiMalformedEventException("No se pudo interpretar", null));

        assertThatThrownBy(() -> service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM)))
                .isInstanceOf(WompiMalformedEventException.class);
        verifyNoInteractions(webhookEventRecorderPort);
    }

    @Test
    @DisplayName("un checksum ya recibido (duplicado) responde sin reprocesar ni persistir de nuevo")
    void checksum_duplicado_no_reprocesa() {
        ParsedWompiEvent evento = evento("transaction.updated", GatewayTransactionStatus.APPROVED,
                null);
        stubAutenticado(evento);
        when(wompiEventPort.computeChecksum(evento)).thenReturn("checksum-computado");
        when(webhookEventRecorderPort.existsByChecksum("WOMPI", "checksum-computado"))
                .thenReturn(true);

        service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

        verify(webhookEventRecorderPort, never()).recordReceived(any(), any(), any(), any(), any(),
                any());
        verifyNoInteractions(firstPeriodPaymentQueryPort, outcomeSettler);
    }

    @Test
    @DisplayName("checksum invalido lanza WompiChecksumMismatchException sin persistir ninguna fila")
    void checksum_invalido_lanza() {
        ParsedWompiEvent evento = evento("transaction.updated", GatewayTransactionStatus.APPROVED,
                null);
        when(wompiEventPort.parse(RAW_BODY)).thenReturn(evento);
        when(wompiEventPort.freshnessTolerance()).thenReturn(Duration.ofHours(24));
        when(wompiEventPort.matchesChecksum(evento, CHECKSUM)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM)))
                .isInstanceOf(WompiChecksumMismatchException.class);

        verifyNoInteractions(webhookEventRecorderPort, firstPeriodPaymentQueryPort, outcomeSettler);
    }

    @Test
    @DisplayName("un timestamp fuera de la ventana de frescura, con checksum valido, persiste REJECTED_STALE")
    void timestamp_viejo_lanza() {
        ParsedWompiEvent evento = evento("transaction.updated", GatewayTransactionStatus.APPROVED,
                null, TIMESTAMP - Duration.ofDays(2).toSeconds(), MONTO_REGISTRADO_EN_CENTAVOS);
        when(wompiEventPort.parse(RAW_BODY)).thenReturn(evento);
        when(wompiEventPort.matchesChecksum(evento, CHECKSUM)).thenReturn(true);
        when(wompiEventPort.freshnessTolerance()).thenReturn(Duration.ofHours(24));
        when(webhookEventRecorderPort.recordReceived(any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM)))
                .isInstanceOf(WompiStaleEventException.class);
        verify(webhookEventRecorderPort).recordReceived(any(), any(), any(), any(), any(), any());
        verify(webhookEventRecorderPort).recordOutcome(eq(1L), any(),
                eq(GatewayWebhookOutcome.REJECTED_STALE));
        verifyNoInteractions(firstPeriodPaymentQueryPort, outcomeSettler);
    }

    @Test
    @DisplayName("un evento que no es transaction.updated se ignora")
    void evento_ajeno_no_hace_nada() {
        ParsedWompiEvent evento = evento("transaction.created", GatewayTransactionStatus.APPROVED,
                null);
        stubAutenticado(evento);

        service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

        verify(webhookEventRecorderPort).recordOutcome(any(), any(),
                eq(GatewayWebhookOutcome.IGNORED_UNKNOWN_EVENT));
        verifyNoInteractions(firstPeriodPaymentQueryPort, outcomeSettler);
    }

    @Nested
    @DisplayName("transaction.updated autenticado")
    class TransactionUpdated {

        @Test
        @DisplayName("una transaccion sin pago conocido no hace nada")
        void pago_desconocido_no_hace_nada() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.APPROVED, null);
            stubAutenticado(evento);
            when(firstPeriodPaymentQueryPort.findByGatewayAndReference("WOMPI", "tx-1"))
                    .thenReturn(Optional.empty());

            service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

            verify(webhookEventRecorderPort).recordOutcome(any(), any(),
                    eq(GatewayWebhookOutcome.PAYMENT_NOT_FOUND));
            verifyNoInteractions(outcomeSettler);
        }

        @Test
        @DisplayName("un pago ya en estado final es idempotente")
        void pago_ya_final_es_idempotente() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.APPROVED, null);
            stubAutenticado(evento);
            when(firstPeriodPaymentQueryPort.findByGatewayAndReference("WOMPI", "tx-1"))
                    .thenReturn(Optional.of(new FirstPeriodPaymentSnapshot(PAGO, EMPRESA,
                            "CONFIRMED", new BigDecimal("45000"), "COP", "tx-1", null)));

            service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

            verify(webhookEventRecorderPort).recordOutcome(any(), any(),
                    eq(GatewayWebhookOutcome.IGNORED_ALREADY_FINAL));
            verifyNoInteractions(outcomeSettler);
        }

        @Test
        @DisplayName("un importe APPROVED distinto al registrado no confirma y deja el pago PENDING")
        void importe_distinto_no_confirma() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.APPROVED, null, TIMESTAMP, 1_000L);
            stubAutenticado(evento);
            when(firstPeriodPaymentQueryPort.findByGatewayAndReference("WOMPI", "tx-1"))
                    .thenReturn(Optional.of(new FirstPeriodPaymentSnapshot(PAGO, EMPRESA, "PENDING",
                            new BigDecimal("45000"), "COP", "tx-1", null)));

            service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

            verify(webhookEventRecorderPort).recordOutcome(any(), any(),
                    eq(GatewayWebhookOutcome.REJECTED_AMOUNT));
            verifyNoInteractions(outcomeSettler, defaultCardPaymentMethodQueryPort,
                    subscriptionPaymentLedgerPort);
        }

        @Test
        @DisplayName("PENDING a APPROVED liquida con el medio de pago y el documento resueltos")
        void pending_a_approved() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.APPROVED, null);
            stubAutenticado(evento);
            when(firstPeriodPaymentQueryPort.findByGatewayAndReference("WOMPI", "tx-1"))
                    .thenReturn(Optional.of(new FirstPeriodPaymentSnapshot(PAGO, EMPRESA, "PENDING",
                            new BigDecimal("45000"), "COP", "tx-1", null)));
            when(defaultCardPaymentMethodQueryPort.findDefaultActiveCard(EMPRESA, "WOMPI"))
                    .thenReturn(Optional.of(new PaymentMethodRef(15L, "9911")));
            when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, PAGO))
                    .thenReturn(Optional.of(900L));

            service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

            verify(outcomeSettler).settle(eq(GatewayTransactionStatus.APPROVED), any(), eq(EMPRESA),
                    eq(PAGO), eq(900L), eq(15L), eq(new BigDecimal("45000")));
            verify(webhookEventRecorderPort).recordOutcome(any(), any(),
                    eq(GatewayWebhookOutcome.APPLIED));
        }

        @Test
        @DisplayName("PENDING a DECLINED liquida igual, aunque ya no haya medio de pago activo")
        void pending_a_declined_sin_medio_de_pago() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.DECLINED, "Fondos insuficientes");
            stubAutenticado(evento);
            when(firstPeriodPaymentQueryPort.findByGatewayAndReference("WOMPI", "tx-1"))
                    .thenReturn(Optional.of(new FirstPeriodPaymentSnapshot(PAGO, EMPRESA, "PENDING",
                            new BigDecimal("45000"), "COP", "tx-1", null)));
            when(defaultCardPaymentMethodQueryPort.findDefaultActiveCard(EMPRESA, "WOMPI"))
                    .thenReturn(Optional.empty());
            when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, PAGO))
                    .thenReturn(Optional.of(900L));

            service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

            verify(outcomeSettler).settle(eq(GatewayTransactionStatus.DECLINED),
                    eq("Fondos insuficientes"), eq(EMPRESA), eq(PAGO), eq(900L), eq((Long) null),
                    eq(new BigDecimal("45000")));
            verify(webhookEventRecorderPort).recordOutcome(any(), any(),
                    eq(GatewayWebhookOutcome.APPLIED));
        }

        @Test
        @DisplayName("durante el settle el MDC lleva actor.type=GATEWAY y la empresa, y se restaura al terminar")
        void pone_actor_gateway_y_empresa_en_el_mdc_durante_el_settle() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.APPROVED, null);
            stubAutenticado(evento);
            when(firstPeriodPaymentQueryPort.findByGatewayAndReference("WOMPI", "tx-1"))
                    .thenReturn(Optional.of(new FirstPeriodPaymentSnapshot(PAGO, EMPRESA, "PENDING",
                            new BigDecimal("45000"), "COP", "tx-1", null)));
            when(defaultCardPaymentMethodQueryPort.findDefaultActiveCard(EMPRESA, "WOMPI"))
                    .thenReturn(Optional.empty());
            when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, PAGO))
                    .thenReturn(Optional.empty());
            AtomicReference<String> actorDuranteElSettle = new AtomicReference<>();
            AtomicReference<String> empresaDuranteElSettle = new AtomicReference<>();
            doAnswer(inv -> {
                actorDuranteElSettle.set(MDC.get(MdcKeys.ACTOR_TYPE));
                empresaDuranteElSettle.set(MDC.get(MdcKeys.ACTOR_COMPANY_ID));
                return null;
            }).when(outcomeSettler).settle(any(), any(), any(), any(), any(), any(), any());

            service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

            assertThat(actorDuranteElSettle.get()).isEqualTo("GATEWAY");
            assertThat(empresaDuranteElSettle.get()).isEqualTo(String.valueOf(EMPRESA));
            assertThat(MDC.get(MdcKeys.ACTOR_TYPE)).isNull();
            assertThat(MDC.get(MdcKeys.ACTOR_COMPANY_ID)).isNull();
        }
    }

    @Nested
    @DisplayName("contador de desenlaces del webhook (#767, #778, #789)")
    class ContadorDeDesenlaces {

        @Test
        @DisplayName("checksum invalido cuenta REJECTED_CHECKSUM sin persistir")
        void checksum_invalido_cuenta() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.APPROVED, null);
            when(wompiEventPort.parse(RAW_BODY)).thenReturn(evento);
            when(wompiEventPort.freshnessTolerance()).thenReturn(Duration.ofHours(24));
            when(wompiEventPort.matchesChecksum(evento, CHECKSUM)).thenReturn(false);

            assertThatThrownBy(
                    () -> service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM)))
                    .isInstanceOf(WompiChecksumMismatchException.class);

            verify(metrics).recordWebhookOutcome(GatewayWebhookOutcome.REJECTED_CHECKSUM);
            verifyNoInteractions(webhookEventRecorderPort);
        }

        @Test
        @DisplayName("un evento autenticado fuera de ventana cuenta REJECTED_STALE")
        void evento_fuera_de_ventana_cuenta_stale() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.APPROVED, null,
                    TIMESTAMP - Duration.ofDays(2).toSeconds(), MONTO_REGISTRADO_EN_CENTAVOS);
            when(wompiEventPort.parse(RAW_BODY)).thenReturn(evento);
            when(wompiEventPort.matchesChecksum(evento, CHECKSUM)).thenReturn(true);
            when(wompiEventPort.freshnessTolerance()).thenReturn(Duration.ofHours(24));
            when(webhookEventRecorderPort.recordReceived(any(), any(), any(), any(), any(), any()))
                    .thenReturn(1L);

            assertThatThrownBy(
                    () -> service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM)))
                    .isInstanceOf(WompiStaleEventException.class);

            verify(metrics).recordWebhookOutcome(GatewayWebhookOutcome.REJECTED_STALE);
        }

        @Test
        @DisplayName("transaccion sin pago conocido cuenta PAYMENT_NOT_FOUND")
        void pago_desconocido_cuenta() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.APPROVED, null);
            stubAutenticado(evento);
            when(firstPeriodPaymentQueryPort.findByGatewayAndReference("WOMPI", "tx-1"))
                    .thenReturn(Optional.empty());

            service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

            verify(metrics).recordWebhookOutcome(GatewayWebhookOutcome.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("un desenlace aplicado cuenta APPLIED")
        void aplicado_cuenta() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.APPROVED, null);
            stubAutenticado(evento);
            when(firstPeriodPaymentQueryPort.findByGatewayAndReference("WOMPI", "tx-1"))
                    .thenReturn(Optional.of(new FirstPeriodPaymentSnapshot(PAGO, EMPRESA, "PENDING",
                            new BigDecimal("45000"), "COP", "tx-1", null)));
            when(defaultCardPaymentMethodQueryPort.findDefaultActiveCard(EMPRESA, "WOMPI"))
                    .thenReturn(Optional.of(new PaymentMethodRef(15L, "9911")));
            when(subscriptionPaymentLedgerPort.findDocumentIdByPayment(EMPRESA, PAGO))
                    .thenReturn(Optional.of(900L));

            service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

            verify(metrics).recordWebhookOutcome(GatewayWebhookOutcome.APPLIED);
        }
    }
}
