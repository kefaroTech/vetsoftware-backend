package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.domain.StalePendingPayment;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaStalePendingPaymentQueryPort")
class JpaStalePendingPaymentQueryPortTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-04T10:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime CORTE = LocalDateTime.now(RELOJ).minusMinutes(60);

    @Mock
    private SubscriptionPaymentRepository repository;

    private JpaStalePendingPaymentQueryPort port;

    private SubscriptionPayment pago(Long id, Long companyId, String gatewayReference,
            LocalDateTime receivedAt) {
        return new SubscriptionPayment(id, companyId, new BigDecimal("45000"), "COP",
                PaymentMethod.CARD, "WOMPI", gatewayReference, receivedAt,
                SubscriptionPaymentStatus.PENDING, null, null, null, null, null, BigDecimal.ZERO,
                "VS-DOC-" + id + "-A1", receivedAt, 0L);
    }

    @Test
    @DisplayName("traduce las filas del repositorio a StalePendingPayment, en el orden recibido")
    void traduce_las_filas_del_repositorio() {
        port = new JpaStalePendingPaymentQueryPort(repository);
        SubscriptionPayment masVieja = pago(501L, 42L, "tx-1", CORTE.minusMinutes(10));
        SubscriptionPayment reserva = pago(502L, 42L, null, CORTE.minusMinutes(5));
        when(repository.findStalePendingGatewayPayments(CORTE, 50))
                .thenReturn(List.of(masVieja, reserva));

        List<StalePendingPayment> stale = port.findOlderThan(CORTE, 50);

        assertThat(stale).extracting(StalePendingPayment::paymentId).containsExactly(501L, 502L);
        assertThat(stale).extracting(StalePendingPayment::gatewayReference).containsExactly("tx-1",
                null);
    }

    @Test
    @DisplayName("sin candidatos: lista vacia")
    void sin_candidatos_lista_vacia() {
        port = new JpaStalePendingPaymentQueryPort(repository);
        when(repository.findStalePendingGatewayPayments(CORTE, 50)).thenReturn(List.of());

        assertThat(port.findOlderThan(CORTE, 50)).isEmpty();
    }
}
