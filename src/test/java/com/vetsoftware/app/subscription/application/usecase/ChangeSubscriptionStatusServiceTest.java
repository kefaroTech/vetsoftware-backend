package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionStatusHistoryRepository;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.InvalidSubscriptionStatusTransitionException;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeSubscriptionStatusService - la transicion y su bitacora")
class ChangeSubscriptionStatusServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);

    @Mock
    private SubscriptionRepository repository;
    @Mock
    private SubscriptionStatusHistoryRepository historyRepository;
    @Mock
    private SubscriptionChangedPort subscriptionChangedPort;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:15:30Z"), ZoneOffset.UTC);

    @InjectMocks
    private ChangeSubscriptionStatusService service;

    private static Subscription contrato(SubscriptionStatus status) {
        return new Subscription(CONTRATO, "SUS-2026-00184", EMPRESA, null, 3L, BillingCycle.MONTHLY,
                status, ENERO_1, null, ENERO_1, LocalDate.of(2026, 1, 31), null, null, 0, null,
                true, null, null, 0L, true);
    }

    private static ChangeSubscriptionStatusCommand comando(SubscriptionStatus destino) {
        return new ChangeSubscriptionStatusCommand(CONTRATO, EMPRESA, destino,
                "Factura FE-1043 vencida hace 6 dias", "billing-job");
    }

    private void contratoEn(SubscriptionStatus status) {
        when(repository.findByIdAndCompanyId(CONTRATO, EMPRESA))
                .thenReturn(Optional.of(contrato(status)));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("Transicion")
    class Transicion {

        @Test
        @DisplayName("cambia el estado y anota la bitacora en la misma operacion")
        void cambiaYAnota() {
            contratoEn(SubscriptionStatus.ACTIVE);

            SubscriptionDto resultado = service.execute(comando(SubscriptionStatus.PAST_DUE));

            assertThat(resultado.status()).isEqualTo(SubscriptionStatus.PAST_DUE);
            ArgumentCaptor<SubscriptionStatusChange> captor = ArgumentCaptor
                    .forClass(SubscriptionStatusChange.class);
            verify(historyRepository).append(captor.capture());
            assertThat(captor.getValue().getFromStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(captor.getValue().getToStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
            assertThat(captor.getValue().getActor()).isEqualTo("billing-job");
        }

        @Test
        @DisplayName("PAST_DUE sigue siendo un contrato vigente: debe, pero sigue trabajando")
        void pastDueSigueVigente() {
            contratoEn(SubscriptionStatus.ACTIVE);

            assertThat(service.execute(comando(SubscriptionStatus.PAST_DUE)).current()).isTrue();
        }

        @Test
        @DisplayName("READ_ONLY es el maximo de restriccion y sigue siendo vigente")
        void readOnlySigueVigente() {
            contratoEn(SubscriptionStatus.PAST_DUE);

            SubscriptionDto resultado = service.execute(comando(SubscriptionStatus.READ_ONLY));

            assertThat(resultado.status()).isEqualTo(SubscriptionStatus.READ_ONLY);
            assertThat(resultado.current()).isTrue();
        }

        @Test
        @DisplayName("una transicion invalida no toca ni la bitacora ni el recalculo")
        void transicionInvalida() {
            when(repository.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato(SubscriptionStatus.CANCELLED)));

            assertThatThrownBy(() -> service.execute(comando(SubscriptionStatus.ACTIVE)))
                    .isInstanceOf(InvalidSubscriptionStatusTransitionException.class);

            verify(repository, never()).save(any());
            verify(historyRepository, never()).append(any());
            verify(subscriptionChangedPort, never()).subscriptionChanged(any());
        }

        @Test
        @DisplayName("un contrato de otra empresa no existe para el caller")
        void contratoAjeno() {
            when(repository.findByIdAndCompanyId(CONTRATO, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(SubscriptionStatus.PAST_DUE)))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Recalculo")
    class Recalculo {

        @Test
        @DisplayName("el paso a PAST_DUE tambien recalcula: ahi se decide si puede escribir")
        void pastDueRecalcula() {
            contratoEn(SubscriptionStatus.ACTIVE);

            service.execute(comando(SubscriptionStatus.PAST_DUE));

            ArgumentCaptor<SubscriptionChangedEvent> captor = ArgumentCaptor
                    .forClass(SubscriptionChangedEvent.class);
            verify(subscriptionChangedPort).subscriptionChanged(captor.capture());
            assertThat(captor.getValue().companyId()).isEqualTo(EMPRESA);
            assertThat(captor.getValue().kind()).isEqualTo(SubscriptionChangeKind.STATUS_CHANGED);
        }

        @Test
        @DisplayName("si el recalculo falla, la excepcion sube y la transaccion revierte")
        void siElRecalculoFallaSube() {
            contratoEn(SubscriptionStatus.ACTIVE);
            doThrow(new IllegalStateException("recalculo caido")).when(subscriptionChangedPort)
                    .subscriptionChanged(any());

            // No se traga el fallo: si los permisos no se pueden recalcular, el cambio de
            // estado tampoco ocurre. No existe el estado intermedio en el que el cliente
            // pago por algo que no puede usar.
            assertThatThrownBy(() -> service.execute(comando(SubscriptionStatus.READ_ONLY)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
