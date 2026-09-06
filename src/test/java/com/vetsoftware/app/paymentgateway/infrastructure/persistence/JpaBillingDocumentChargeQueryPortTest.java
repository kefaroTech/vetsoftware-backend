package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.domain.BillingDocumentChargeSnapshot;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code SubscriptionBillingDocumentJpaEntity} se mockea: su constructor sin
 * argumentos es protegido.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBillingDocumentChargeQueryPort")
class JpaBillingDocumentChargeQueryPortTest {

    private static final Long EMPRESA = 42L;
    private static final Long DOCUMENTO = 900L;

    @Mock
    private SubscriptionBillingDocumentJpaRepository repository;
    @InjectMocks
    private JpaBillingDocumentChargeQueryPort port;

    @Test
    @DisplayName("mapea el documento a snapshot con la moneda fija en COP")
    void mapea_el_documento_a_snapshot() {
        SubscriptionBillingDocumentJpaEntity entity = mock(
                SubscriptionBillingDocumentJpaEntity.class);
        when(entity.getId()).thenReturn(DOCUMENTO);
        when(entity.getDocumentNumber()).thenReturn("FV-1");
        when(entity.getTotalAmount()).thenReturn(new BigDecimal("45000"));
        when(entity.getBalanceAmount()).thenReturn(new BigDecimal("30000"));
        when(entity.getSubscriptionId()).thenReturn(7L);
        when(repository.findByIdAndCompanyId(DOCUMENTO, EMPRESA)).thenReturn(Optional.of(entity));

        Optional<BillingDocumentChargeSnapshot> snapshot = port.findByIdAndCompanyId(DOCUMENTO,
                EMPRESA);

        assertThat(snapshot).contains(new BillingDocumentChargeSnapshot(DOCUMENTO, "FV-1",
                new BigDecimal("45000"), new BigDecimal("30000"), "COP", 7L));
    }

    @Test
    @DisplayName("devuelve vacio si el documento no existe para esa empresa")
    void devuelve_vacio_si_no_existe() {
        when(repository.findByIdAndCompanyId(DOCUMENTO, EMPRESA)).thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(DOCUMENTO, EMPRESA)).isEmpty();
    }
}
