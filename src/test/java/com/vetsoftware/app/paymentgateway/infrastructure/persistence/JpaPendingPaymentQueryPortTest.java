package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptQueryPort;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.LastPaymentAttempt;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.BillingDocumentApplicationJpaEntity;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.BillingDocumentApplicationJpaRepository;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.SubscriptionPaymentJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaPendingPaymentQueryPort")
class JpaPendingPaymentQueryPortTest {

    private static final Long EMPRESA = 42L;
    private static final Long DOCUMENTO = 900L;
    private static final LocalDateTime RECIBIDO = LocalDateTime.of(2026, 3, 1, 10, 0);

    @Mock
    private BillingDocumentApplicationJpaRepository repository;
    @Mock
    private PaymentAttemptQueryPort paymentAttemptQueryPort;
    @InjectMocks
    private JpaPendingPaymentQueryPort port;

    private static BillingDocumentApplicationJpaEntity aplicacion(ApplicationSourceKind sourceKind,
            SubscriptionPaymentStatus paymentStatus) {
        return aplicacion(sourceKind, paymentStatus, null);
    }

    private static BillingDocumentApplicationJpaEntity aplicacion(ApplicationSourceKind sourceKind,
            SubscriptionPaymentStatus paymentStatus, LocalDateTime receivedAt) {
        BillingDocumentApplicationJpaEntity entity = mock(
                BillingDocumentApplicationJpaEntity.class);
        when(entity.getSourceKind()).thenReturn(sourceKind);
        if (paymentStatus != null) {
            SubscriptionPaymentJpaEntity payment = mock(SubscriptionPaymentJpaEntity.class);
            when(payment.getStatus()).thenReturn(paymentStatus);
            if (paymentStatus == SubscriptionPaymentStatus.PENDING) {
                when(payment.getReceivedAt()).thenReturn(receivedAt);
            }
            when(entity.getPayment()).thenReturn(payment);
        }
        return entity;
    }

    private void paginaCon(BillingDocumentApplicationJpaEntity... aplicaciones) {
        Page<BillingDocumentApplicationJpaEntity> pagina = new PageImpl<>(List.of(aplicaciones));
        when(repository.findAllByTargetDocument_IdAndCompanyId(DOCUMENTO, EMPRESA,
                Pageable.unpaged())).thenReturn(pagina);
    }

    @Test
    @DisplayName("hay un pago PENDING aplicado: true")
    void hay_un_pago_pendiente_aplicado() {
        paginaCon(aplicacion(ApplicationSourceKind.PAYMENT, SubscriptionPaymentStatus.PENDING,
                RECIBIDO));

        assertThat(port.existsPendingPayment(EMPRESA, DOCUMENTO)).isTrue();
    }

    @Test
    @DisplayName("el PENDING ya tiene un intento anotado despues de recibirse (RES2-29): false")
    void pending_con_intento_posterior_no_bloquea() {
        paginaCon(aplicacion(ApplicationSourceKind.PAYMENT, SubscriptionPaymentStatus.PENDING,
                RECIBIDO));
        when(paymentAttemptQueryPort.findLast(EMPRESA, DOCUMENTO)).thenReturn(Optional.of(
                new LastPaymentAttempt(1, GatewayDeclineKind.SOFT, RECIBIDO.plusHours(1), null)));

        assertThat(port.existsPendingPayment(EMPRESA, DOCUMENTO)).isFalse();
    }

    @Test
    @DisplayName("el PENDING tiene un intento previo a que se recibiera: sigue bloqueando")
    void pending_con_intento_anterior_sigue_bloqueando() {
        paginaCon(aplicacion(ApplicationSourceKind.PAYMENT, SubscriptionPaymentStatus.PENDING,
                RECIBIDO));
        when(paymentAttemptQueryPort.findLast(EMPRESA, DOCUMENTO)).thenReturn(Optional.of(
                new LastPaymentAttempt(1, GatewayDeclineKind.SOFT, RECIBIDO.minusDays(1), null)));

        assertThat(port.existsPendingPayment(EMPRESA, DOCUMENTO)).isTrue();
    }

    @Test
    @DisplayName("el pago aplicado ya esta CONFIRMED: false")
    void el_pago_aplicado_ya_esta_confirmado() {
        paginaCon(aplicacion(ApplicationSourceKind.PAYMENT, SubscriptionPaymentStatus.CONFIRMED));

        assertThat(port.existsPendingPayment(EMPRESA, DOCUMENTO)).isFalse();
    }

    @Test
    @DisplayName("una aplicacion de nota credito no cuenta aunque el documento tenga otras filas")
    void una_aplicacion_de_credit_note_no_cuenta() {
        paginaCon(aplicacion(ApplicationSourceKind.CREDIT_NOTE, null));

        assertThat(port.existsPendingPayment(EMPRESA, DOCUMENTO)).isFalse();
    }

    @Test
    @DisplayName("sin aplicaciones: false")
    void sin_aplicaciones() {
        paginaCon();

        assertThat(port.existsPendingPayment(EMPRESA, DOCUMENTO)).isFalse();
    }
}
