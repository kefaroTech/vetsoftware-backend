package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.domain.RecurringChargeTarget;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaNewRecurringChargeQueryPort")
class JpaNewRecurringChargeQueryPortTest {

    @Mock
    private NewRecurringChargeJpaRepository repository;
    @InjectMocks
    private JpaNewRecurringChargeQueryPort port;

    @Test
    @DisplayName("mapea cada fila a (companyId, billingDocumentId), sin mas datos")
    void mapea_cada_fila_a_su_objetivo() {
        SubscriptionBillingDocumentJpaEntity uno = mock(SubscriptionBillingDocumentJpaEntity.class);
        when(uno.getCompanyId()).thenReturn(42L);
        when(uno.getId()).thenReturn(900L);
        SubscriptionBillingDocumentJpaEntity dos = mock(SubscriptionBillingDocumentJpaEntity.class);
        when(dos.getCompanyId()).thenReturn(43L);
        when(dos.getId()).thenReturn(901L);
        when(repository.findNewRecurringChargesAfter(0L, 100)).thenReturn(List.of(uno, dos));

        List<RecurringChargeTarget> targets = port.findAfter(0L, 100);

        assertThat(targets).containsExactly(new RecurringChargeTarget(42L, 900L),
                new RecurringChargeTarget(43L, 901L));
    }

    @Test
    @DisplayName("sin candidatos: lista vacia")
    void sin_candidatos() {
        when(repository.findNewRecurringChargesAfter(500L, 100)).thenReturn(List.of());

        assertThat(port.findAfter(500L, 100)).isEmpty();
    }
}
