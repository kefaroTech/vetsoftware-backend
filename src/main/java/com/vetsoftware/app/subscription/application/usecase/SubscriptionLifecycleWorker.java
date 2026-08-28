package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionLifecycleBatchResult;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionStatusUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.domain.CancellationRequest;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChangeReason;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hace efectivos los cambios que dependen exclusivamente del paso del tiempo.
 * El lote y sus filas permanecen en la misma transaccion: estado, historia y
 * entitlements se confirman o revierten juntos.
 */
@Service
public class SubscriptionLifecycleWorker {

    private static final String ACTOR = "SYSTEM:SUBSCRIPTION_LIFECYCLE";

    private final SubscriptionRepository repository;
    private final ChangeSubscriptionStatusUseCase changeStatusUseCase;
    private final SubscriptionChangedPort subscriptionChangedPort;
    private final Clock clock;

    public SubscriptionLifecycleWorker(SubscriptionRepository repository,
            ChangeSubscriptionStatusUseCase changeStatusUseCase,
            SubscriptionChangedPort subscriptionChangedPort, Clock clock) {
        this.repository = repository;
        this.changeStatusUseCase = changeStatusUseCase;
        this.subscriptionChangedPort = subscriptionChangedPort;
        this.clock = clock;
    }

    @Transactional
    public SubscriptionLifecycleBatchResult processBatchAfter(long afterId, int batchSize) {
        if (afterId < 0)
            throw new IllegalArgumentException("afterId must not be negative");
        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize must be positive");

        List<Subscription> subscriptions = repository.lockLifecycleBatchAfter(afterId, batchSize);
        LocalDate today = LocalDate.now(clock);
        for (Subscription subscription : subscriptions) {
            process(subscription, today);
        }

        long lastId = subscriptions.isEmpty()
                ? afterId
                : subscriptions.get(subscriptions.size() - 1).getId();
        return new SubscriptionLifecycleBatchResult(subscriptions.size(), lastId);
    }

    /**
     * <b>Los motivos ya no se concatenan.</b> Antes salian de aqui frases como
     * «Periodo de prueba finalizado el 2026-01-14» que acababan en la columna
     * {@code reason} de la bitacora y en el canal de auditoria, junto al texto que
     * el cliente escribia por HTTP: dos fuentes distintas mezcladas en el mismo
     * campo, una de ellas ajena. La fecha que llevaban dentro no se pierde —esta en
     * {@code occurredAt} de la propia fila y en el contrato—, y lo que se gana es
     * que el campo sea agrupable y no falsificable.
     */
    private void process(Subscription subscription, LocalDate today) {
        // La consulta del adaptador ya excluye los estados terminales. Esta guarda
        // mantiene idempotente al worker incluso ante un repositorio alternativo o
        // un lote construido en memoria: una cancelacion conserva su solicitud y
        // no debe intentar CANCELLED -> CANCELLED en cada barrido.
        if (subscription.getStatus().isTerminal()) {
            return;
        }
        CancellationRequest cancellation = subscription.getCancellation();
        if (cancellation != null && cancellation.hasTakenEffectOn(today)) {
            changeStatusUseCase.execute(new ChangeSubscriptionStatusCommand(subscription.getId(),
                    subscription.getCompanyId(), SubscriptionStatus.CANCELLED,
                    SubscriptionStatusChangeReason.CANCELLATION_EFFECTIVE, ACTOR));
            return;
        }

        if (subscription.getStatus() == SubscriptionStatus.TRIALING
                && subscription.getTrialEndDate().isBefore(today)) {
            changeStatusUseCase.execute(new ChangeSubscriptionStatusCommand(subscription.getId(),
                    subscription.getCompanyId(), SubscriptionStatus.ACTIVE,
                    SubscriptionStatusChangeReason.TRIAL_ENDED, ACTOR));
            return;
        }

        subscriptionChangedPort.subscriptionChanged(
                new SubscriptionChangedEvent(subscription.getCompanyId(), subscription.getId(),
                        SubscriptionChangeKind.EFFECTIVE_DATE_REACHED, today));
    }
}
