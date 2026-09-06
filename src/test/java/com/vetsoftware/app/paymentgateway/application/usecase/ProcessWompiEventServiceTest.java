package com.vetsoftware.app.paymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.command.ProcessWompiEventCommand;
import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.FirstPeriodPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.application.port.out.WompiEventPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.ParsedWompiEvent;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import com.vetsoftware.app.paymentgateway.domain.WompiChecksumMismatchException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessWompiEventService")
class ProcessWompiEventServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long PAGO = 501L;
    private static final String RAW_BODY = "{}";
    private static final String CHECKSUM = "abc123";

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

    private ProcessWompiEventService service;

    @BeforeEach
    void setUp() {
        service = new ProcessWompiEventService(wompiEventPort, firstPeriodPaymentQueryPort,
                defaultCardPaymentMethodQueryPort, subscriptionPaymentLedgerPort, outcomeSettler);
    }

    private ParsedWompiEvent evento(String tipo, GatewayTransactionStatus estado, String mensaje) {
        return new ParsedWompiEvent(tipo, "tx-1", estado, mensaje, 1530291411L, List.of("a", "b"));
    }

    @Test
    @DisplayName("checksum invalido lanza WompiChecksumMismatchException")
    void checksum_invalido_lanza() {
        ParsedWompiEvent evento = evento("transaction.updated", GatewayTransactionStatus.APPROVED,
                null);
        when(wompiEventPort.parse(RAW_BODY)).thenReturn(evento);
        when(wompiEventPort.matchesChecksum(evento, CHECKSUM)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM)))
                .isInstanceOf(WompiChecksumMismatchException.class);
        verifyNoInteractions(firstPeriodPaymentQueryPort, outcomeSettler);
    }

    @Test
    @DisplayName("un evento que no es transaction.updated se ignora")
    void evento_ajeno_no_hace_nada() {
        ParsedWompiEvent evento = evento("transaction.created", GatewayTransactionStatus.APPROVED,
                null);
        when(wompiEventPort.parse(RAW_BODY)).thenReturn(evento);
        when(wompiEventPort.matchesChecksum(evento, CHECKSUM)).thenReturn(true);

        service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

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
            when(wompiEventPort.parse(RAW_BODY)).thenReturn(evento);
            when(wompiEventPort.matchesChecksum(evento, CHECKSUM)).thenReturn(true);
            when(firstPeriodPaymentQueryPort.findByGatewayAndReference("WOMPI", "tx-1"))
                    .thenReturn(Optional.empty());

            service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

            verifyNoInteractions(outcomeSettler);
        }

        @Test
        @DisplayName("un pago ya en estado final es idempotente")
        void pago_ya_final_es_idempotente() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.APPROVED, null);
            when(wompiEventPort.parse(RAW_BODY)).thenReturn(evento);
            when(wompiEventPort.matchesChecksum(evento, CHECKSUM)).thenReturn(true);
            when(firstPeriodPaymentQueryPort.findByGatewayAndReference("WOMPI", "tx-1"))
                    .thenReturn(Optional.of(new FirstPeriodPaymentSnapshot(PAGO, EMPRESA,
                            "CONFIRMED", new BigDecimal("45000"), "COP", "tx-1", null)));

            service.execute(new ProcessWompiEventCommand(RAW_BODY, CHECKSUM));

            verifyNoInteractions(outcomeSettler);
        }

        @Test
        @DisplayName("PENDING a APPROVED liquida con el medio de pago y el documento resueltos")
        void pending_a_approved() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.APPROVED, null);
            when(wompiEventPort.parse(RAW_BODY)).thenReturn(evento);
            when(wompiEventPort.matchesChecksum(evento, CHECKSUM)).thenReturn(true);
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
        }

        @Test
        @DisplayName("PENDING a DECLINED liquida igual, aunque ya no haya medio de pago activo")
        void pending_a_declined_sin_medio_de_pago() {
            ParsedWompiEvent evento = evento("transaction.updated",
                    GatewayTransactionStatus.DECLINED, "Fondos insuficientes");
            when(wompiEventPort.parse(RAW_BODY)).thenReturn(evento);
            when(wompiEventPort.matchesChecksum(evento, CHECKSUM)).thenReturn(true);
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
        }
    }
}
