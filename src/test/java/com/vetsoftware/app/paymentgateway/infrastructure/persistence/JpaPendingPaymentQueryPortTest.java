package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.BillingDocumentApplicationJpaEntity;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.BillingDocumentApplicationJpaRepository;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.SubscriptionPaymentJpaEntity;
import java.util.List;
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

    @Mock
    private BillingDocumentApplicationJpaRepository repository;
    @InjectMocks
    private JpaPendingPaymentQueryPort port;

    private static BillingDocumentApplicationJpaEntity aplicacion(ApplicationSourceKind sourceKind,
            SubscriptionPaymentStatus paymentStatus) {
        BillingDocumentApplicationJpaEntity entity = mock(
                BillingDocumentApplicationJpaEntity.class);
        when(entity.getSourceKind()).thenReturn(sourceKind);
        if (paymentStatus != null) {
            SubscriptionPaymentJpaEntity payment = mock(SubscriptionPaymentJpaEntity.class);
            when(payment.getStatus()).thenReturn(paymentStatus);
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
        paginaCon(aplicacion(ApplicationSourceKind.PAYMENT, SubscriptionPaymentStatus.PENDING));

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
