package com.vetsoftware.app.dunning.application.usecase;

import com.vetsoftware.app.dunning.application.port.in.EvaluateDunningUseCase;
import com.vetsoftware.app.dunning.application.port.out.DunningBillingDocumentPort;
import com.vetsoftware.app.dunning.application.port.out.DunningEventRepository;
import com.vetsoftware.app.dunning.application.port.out.DunningSubscriptionPort;
import com.vetsoftware.app.dunning.domain.DunningBillingDocumentSnapshot;
import com.vetsoftware.app.dunning.domain.DunningEvent;
import com.vetsoftware.app.dunning.domain.DunningEventType;
import com.vetsoftware.app.dunning.domain.DunningSubscriptionSnapshot;
import com.vetsoftware.app.dunning.domain.DunningSubscriptionStatus;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decide la mora desde la deuda externa real, nunca desde un temporizador
 * local. Documento y contrato quedan bloqueados durante decisión, historia,
 * evento y recálculo de entitlements.
 */
@Observed(name = "subscription.dunning.evaluate")
@Service
public class DunningEvaluationService implements EvaluateDunningUseCase {

    private static final String ACTOR = "SYSTEM:DUNNING";

    private final DunningBillingDocumentPort billingDocumentPort;
    private final DunningSubscriptionPort subscriptionPort;
    private final DunningEventRepository eventRepository;
    private final Clock clock;

    public DunningEvaluationService(DunningBillingDocumentPort billingDocumentPort,
            DunningSubscriptionPort subscriptionPort, DunningEventRepository eventRepository,
            Clock clock) {
        this.billingDocumentPort = billingDocumentPort;
        this.subscriptionPort = subscriptionPort;
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void evaluate(Long billingDocumentId, Long companyId) {
        DunningBillingDocumentSnapshot trigger = billingDocumentPort
                .lockByIdAndCompanyId(billingDocumentId, companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Billing document not found: " + billingDocumentId));
        DunningSubscriptionSnapshot subscription = subscriptionPort
                .lockByIdAndCompanyId(trigger.subscriptionId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + trigger.subscriptionId()));

        if (subscription.status().isTerminal()) {
            return;
        }

        LocalDate today = LocalDate.now(clock);
        Optional<DunningBillingDocumentSnapshot> oldest = billingDocumentPort
                .findOldestOverdue(trigger.subscriptionId(), companyId, today);
        if (oldest.isEmpty()) {
            reactivateIfNeeded(subscription, today);
            return;
        }

        applyDelinquency(subscription, oldest.get(), today);
    }

    private void applyDelinquency(DunningSubscriptionSnapshot subscription,
            DunningBillingDocumentSnapshot overdue, LocalDate today) {
        int daysOverdue = Math.toIntExact(ChronoUnit.DAYS.between(overdue.dueDate(), today));
        DunningSubscriptionStatus current = subscription.status();

        if (current == DunningSubscriptionStatus.ACTIVE
                || current == DunningSubscriptionStatus.TRIALING) {
            String reason = "Factura " + overdue.document().documentNumber() + " vencida hace "
                    + daysOverdue + " dias; inicia periodo de gracia";
            transition(subscription, DunningSubscriptionStatus.PAST_DUE, reason,
                    DunningEventType.GRACE_STARTED, overdue, daysOverdue);
            current = DunningSubscriptionStatus.PAST_DUE;
        }

        if (current == DunningSubscriptionStatus.PAST_DUE
                && daysOverdue > subscription.graceDays()) {
            String reason = "Factura " + overdue.document().documentNumber() + " vencida hace "
                    + daysOverdue + " dias; gracia de " + subscription.graceDays()
                    + " dias agotada";
            transition(subscription, DunningSubscriptionStatus.READ_ONLY, reason,
                    DunningEventType.READ_ONLY_APPLIED, null, daysOverdue);
        }
    }

    private void reactivateIfNeeded(DunningSubscriptionSnapshot subscription, LocalDate today) {
        if (subscription.status() != DunningSubscriptionStatus.PAST_DUE
                && subscription.status() != DunningSubscriptionStatus.READ_ONLY) {
            return;
        }
        String reason = "Sin facturas externas vencidas con saldo pendiente al " + today;
        transition(subscription, DunningSubscriptionStatus.ACTIVE, reason,
                DunningEventType.REACTIVATED, null, null);
    }

    private void transition(DunningSubscriptionSnapshot subscription,
            DunningSubscriptionStatus target, String reason, DunningEventType eventType,
            DunningBillingDocumentSnapshot document, Integer daysOverdue) {
        subscriptionPort.changeStatus(subscription.subscription().id(),
                subscription.subscription().companyId(), target, reason, ACTOR);

        LocalDateTime occurredAt = LocalDateTime.now(clock);
        eventRepository.save(DunningEvent.record(subscription.subscription().companyId(),
                subscription.subscription(), document == null ? null : document.document(),
                eventType, daysOverdue, null, reason, occurredAt, occurredAt));
    }
}
