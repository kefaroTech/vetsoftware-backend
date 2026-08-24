package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionLifecycleBatchResult;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionStatusUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.CancellationRequest;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionLifecycleWorker — fechas efectivas repetibles")
class SubscriptionLifecycleWorkerTest {

    private static final LocalDate HOY = LocalDate.of(2026, 1, 15);

    @Mock
    private SubscriptionRepository repository;
    @Mock
    private ChangeSubscriptionStatusUseCase changeStatusUseCase;
    @Mock
    private SubscriptionChangedPort subscriptionChangedPort;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:15:30Z"), ZoneOffset.UTC);
    @InjectMocks
    private SubscriptionLifecycleWorker worker;

    @Test
    @DisplayName("la cancelación cambia a CANCELLED justo en su fecha efectiva")
    void la_cancelacion_cambia_a_cancelled_justo_en_su_fecha_efectiva() {
        CancellationRequest cancellation = new CancellationRequest(
                LocalDateTime.of(2026, 1, 10, 9, 0), HOY, "cierre");
        when(repository.lockLifecycleBatchAfter(0, 20)).thenReturn(
                List.of(subscription(7L, SubscriptionStatus.ACTIVE, null, cancellation)));

        worker.processBatchAfter(0, 20);

        ArgumentCaptor<ChangeSubscriptionStatusCommand> command = ArgumentCaptor
                .forClass(ChangeSubscriptionStatusCommand.class);
        verify(changeStatusUseCase).execute(command.capture());
        assertThat(command.getValue().id()).isEqualTo(7L);
        assertThat(command.getValue().companyId()).isEqualTo(42L);
        assertThat(command.getValue().status()).isEqualTo(SubscriptionStatus.CANCELLED);
        verifyNoInteractions(subscriptionChangedPort);
    }

    @Test
    @DisplayName("el trial cambia a ACTIVE al día siguiente y no antes")
    void el_trial_cambia_a_active_al_dia_siguiente_y_no_antes() {
        when(repository.lockLifecycleBatchAfter(0, 20)).thenReturn(
                List.of(subscription(7L, SubscriptionStatus.TRIALING, HOY.minusDays(1), null),
                        subscription(8L, SubscriptionStatus.TRIALING, HOY, null)));

        SubscriptionLifecycleBatchResult result = worker.processBatchAfter(0, 20);

        ArgumentCaptor<ChangeSubscriptionStatusCommand> command = ArgumentCaptor
                .forClass(ChangeSubscriptionStatusCommand.class);
        verify(changeStatusUseCase).execute(command.capture());
        assertThat(command.getValue().id()).isEqualTo(7L);
        assertThat(command.getValue().status()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionChangedPort).subscriptionChanged(new SubscriptionChangedEvent(42L, 8L,
                SubscriptionChangeKind.EFFECTIVE_DATE_REACHED, HOY));
        assertThat(result.processed()).isEqualTo(2);
        assertThat(result.lastId()).isEqualTo(8L);
    }

    @Test
    @DisplayName("sin transición publica el evento diario que recalcula las vigencias de líneas")
    void sin_transicion_publica_el_evento_diario_de_vigencias() {
        when(repository.lockLifecycleBatchAfter(11, 5))
                .thenReturn(List.of(subscription(12L, SubscriptionStatus.ACTIVE, null, null)));

        worker.processBatchAfter(11, 5);

        verify(changeStatusUseCase, never()).execute(any());
        verify(subscriptionChangedPort).subscriptionChanged(new SubscriptionChangedEvent(42L, 12L,
                SubscriptionChangeKind.EFFECTIVE_DATE_REACHED, HOY));
    }

    @Test
    @DisplayName("un lote vacío conserva el cursor para terminar sin repetir filas")
    void un_lote_vacio_conserva_el_cursor() {
        when(repository.lockLifecycleBatchAfter(91, 10)).thenReturn(List.of());

        SubscriptionLifecycleBatchResult result = worker.processBatchAfter(91, 10);

        assertThat(result.processed()).isZero();
        assertThat(result.lastId()).isEqualTo(91L);
        verifyNoInteractions(changeStatusUseCase, subscriptionChangedPort);
    }

    @Test
    @DisplayName("reprocesar una suscripción terminal no duplica transición ni evento")
    void reprocesar_una_suscripcion_terminal_no_duplica_efectos() {
        CancellationRequest cancellation = new CancellationRequest(
                LocalDateTime.of(2026, 1, 10, 9, 0), HOY.minusDays(1), "cierre");
        when(repository.lockLifecycleBatchAfter(0, 20)).thenReturn(
                List.of(subscription(7L, SubscriptionStatus.CANCELLED, null, cancellation)));

        SubscriptionLifecycleBatchResult result = worker.processBatchAfter(0, 20);

        assertThat(result.processed()).isEqualTo(1);
        assertThat(result.lastId()).isEqualTo(7L);
        verifyNoInteractions(changeStatusUseCase, subscriptionChangedPort);
    }

    @Test
    @DisplayName("rechaza cursores negativos y lotes vacíos antes de consultar")
    void rechaza_parametros_invalidos_antes_de_consultar() {
        assertThatThrownBy(() -> worker.processBatchAfter(-1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> worker.processBatchAfter(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    private static Subscription subscription(Long id, SubscriptionStatus status, LocalDate trialEnd,
            CancellationRequest cancellation) {
        return new Subscription(id, "SUS-2026-" + id, 42L, null, 3L, BillingCycle.MONTHLY, status,
                LocalDate.of(2026, 1, 1), trialEnd, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 1), null, 5, null, true,
                cancellation, null, 0L, true);
    }
}
