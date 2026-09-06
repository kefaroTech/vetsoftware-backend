package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import com.vetsoftware.app.paymentattempt.infrastructure.persistence.PaymentAttemptJpaEntity;
import com.vetsoftware.app.paymentattempt.infrastructure.persistence.PaymentAttemptJpaRepository;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.LastPaymentAttempt;
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
@DisplayName("JpaPaymentAttemptQueryPort")
class JpaPaymentAttemptQueryPortTest {

    private static final Long EMPRESA = 42L;
    private static final Long DOCUMENTO = 900L;
    private static final LocalDateTime ATTEMPTED_AT = LocalDateTime.of(2026, 3, 4, 8, 0);
    private static final LocalDateTime NEXT_ATTEMPT_AT = ATTEMPTED_AT.plusDays(1);

    @Mock
    private PaymentAttemptJpaRepository repository;
    @InjectMocks
    private JpaPaymentAttemptQueryPort port;

    @Test
    @DisplayName("mapea el ultimo intento (primero de la pagina) a LastPaymentAttempt")
    void mapea_el_ultimo_intento() {
        PaymentAttemptJpaEntity entity = mock(PaymentAttemptJpaEntity.class);
        when(entity.getAttemptNumber()).thenReturn(2);
        when(entity.getDeclineKind()).thenReturn(DeclineKind.SOFT);
        when(entity.getAttemptedAt()).thenReturn(ATTEMPTED_AT);
        when(entity.getNextAttemptAt()).thenReturn(NEXT_ATTEMPT_AT);
        Page<PaymentAttemptJpaEntity> pagina = new PageImpl<>(List.of(entity));
        when(repository.findAllByCompanyIdAndBillingDocumentId(eq(EMPRESA), eq(DOCUMENTO),
                any(Pageable.class))).thenReturn(pagina);

        Optional<LastPaymentAttempt> ultimo = port.findLast(EMPRESA, DOCUMENTO);

        assertThat(ultimo).contains(
                new LastPaymentAttempt(2, GatewayDeclineKind.SOFT, ATTEMPTED_AT, NEXT_ATTEMPT_AT));
    }

    @Test
    @DisplayName("pide la pagina ordenada por numero de intento descendente, tamano 1")
    void pide_la_pagina_ordenada_descendente() {
        when(repository.findAllByCompanyIdAndBillingDocumentId(eq(EMPRESA), eq(DOCUMENTO),
                any(Pageable.class))).thenReturn(Page.empty());

        port.findLast(EMPRESA, DOCUMENTO);

        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor
                .forClass(Pageable.class);
        verify(repository).findAllByCompanyIdAndBillingDocumentId(eq(EMPRESA), eq(DOCUMENTO),
                captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
        assertThat(captor.getValue().getSort().getOrderFor("attemptNumber").isDescending())
                .isTrue();
    }

    @Test
    @DisplayName("sin intentos: vacio")
    void sin_intentos() {
        when(repository.findAllByCompanyIdAndBillingDocumentId(eq(EMPRESA), eq(DOCUMENTO),
                any(Pageable.class))).thenReturn(Page.empty());

        assertThat(port.findLast(EMPRESA, DOCUMENTO)).isEmpty();
    }

    @Test
    @DisplayName("countRetryableSince delega y excluye CONFIGURATION")
    void count_retryable_since_excluye_configuration() {
        LocalDateTime since = ATTEMPTED_AT.minusDays(14);
        when(repository.countChargeableSince(EMPRESA, DOCUMENTO, since, DeclineKind.CONFIGURATION))
                .thenReturn(3L);

        int count = port.countRetryableSince(EMPRESA, DOCUMENTO, since);

        assertThat(count).isEqualTo(3);
    }
}
