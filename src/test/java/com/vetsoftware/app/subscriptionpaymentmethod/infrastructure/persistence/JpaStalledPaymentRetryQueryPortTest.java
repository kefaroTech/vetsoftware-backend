package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import com.vetsoftware.app.paymentattempt.infrastructure.persistence.PaymentAttemptJpaEntity;
import com.vetsoftware.app.paymentattempt.infrastructure.persistence.PaymentAttemptJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code PaymentAttemptJpaEntity} se mockea: su constructor sin argumentos es
 * protegido.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaStalledPaymentRetryQueryPort")
class JpaStalledPaymentRetryQueryPortTest {

    private static final Long EMPRESA = 42L;

    @Mock
    private PaymentAttemptJpaRepository paymentAttemptJpaRepository;
    @InjectMocks
    private JpaStalledPaymentRetryQueryPort port;

    @Test
    @DisplayName("trae los ids de los ultimos intentos CONFIGURATION y HARD con saldo")
    void trae_los_ids_de_los_ultimos_varados() {
        PaymentAttemptJpaEntity configuracion = mock(PaymentAttemptJpaEntity.class);
        when(configuracion.getId()).thenReturn(501L);
        PaymentAttemptJpaEntity duro = mock(PaymentAttemptJpaEntity.class);
        when(duro.getId()).thenReturn(900L);
        when(paymentAttemptJpaRepository.findLastAttemptsByCompanyIdAndDeclineKind(EMPRESA,
                DeclineKind.CONFIGURATION)).thenReturn(List.of(configuracion));
        when(paymentAttemptJpaRepository.findLastAttemptsByCompanyIdAndDeclineKind(EMPRESA,
                DeclineKind.HARD)).thenReturn(List.of(duro));

        assertThat(port.findStalledLastAttemptIds(EMPRESA)).containsExactly(501L, 900L);
    }

    @Test
    @DisplayName("sin intentos de ninguna clase devuelve lista vacia")
    void sin_intentos_devuelve_vacio() {
        when(paymentAttemptJpaRepository.findLastAttemptsByCompanyIdAndDeclineKind(EMPRESA,
                DeclineKind.CONFIGURATION)).thenReturn(List.of());
        when(paymentAttemptJpaRepository.findLastAttemptsByCompanyIdAndDeclineKind(EMPRESA,
                DeclineKind.HARD)).thenReturn(List.of());

        assertThat(port.findStalledLastAttemptIds(EMPRESA)).isEmpty();
    }
}
