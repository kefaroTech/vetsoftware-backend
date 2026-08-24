package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionStatusUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionStatusHistoryRepository;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transicion de estado, siempre anotada en la bitacora. Las dos cosas salen del
 * mismo metodo del dominio para que no puedan separarse: una cuenta en solo
 * lectura sin su fila de historia es una cuenta que nadie sabe explicar.
 *
 * <p>
 * <strong>El estado maximo de restriccion es {@code READ_ONLY}</strong>, y esto
 * es politica de producto, no una omision (R18). No existe ni debe
 * implementarse un estado de corte total de acceso: un cliente moroso nunca
 * puede quedarse sin poder consultar su propia historia clinica, que es un
 * riesgo legal real. Quien quiera anadir ese estado tiene que anadirlo al enum
 * {@code SubscriptionStatus}, y ahi le espera el test que lo impide.
 */
@Observed(name = "subscription.status.change")
@Service
public class ChangeSubscriptionStatusService implements ChangeSubscriptionStatusUseCase {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final SubscriptionRepository repository;
    private final SubscriptionStatusHistoryRepository historyRepository;
    private final SubscriptionChangedPort subscriptionChangedPort;
    private final Clock clock;

    public ChangeSubscriptionStatusService(SubscriptionRepository repository,
            SubscriptionStatusHistoryRepository historyRepository,
            SubscriptionChangedPort subscriptionChangedPort, Clock clock) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.subscriptionChangedPort = subscriptionChangedPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionDto execute(ChangeSubscriptionStatusCommand command) {
        Subscription subscription = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.id()));

        LocalDateTime occurredAt = LocalDateTime.now(clock);
        SubscriptionStatusChange change = subscription.changeStatus(command.status(),
                command.reason(), actorOf(command), occurredAt);

        Subscription saved = repository.save(subscription);
        historyRepository.append(new SubscriptionStatusChange(null, saved.getCompanyId(),
                saved.getId(), change.getFromStatus(), change.getToStatus(), change.getReason(),
                change.getOccurredAt(), change.getActor(), null));

        // R11 exige recalcular tambien en el paso a PAST_DUE y a READ_ONLY, no solo en
        // altas y bajas de linea: es donde se decide si el cliente puede escribir.
        subscriptionChangedPort.subscriptionChanged(
                new SubscriptionChangedEvent(saved.getCompanyId(), saved.getId(),
                        SubscriptionChangeKind.STATUS_CHANGED, occurredAt.toLocalDate()));

        return SubscriptionDto.from(saved);
    }

    private static String actorOf(ChangeSubscriptionStatusCommand command) {
        return command.actor() == null || command.actor().isBlank()
                ? SYSTEM_ACTOR
                : command.actor();
    }
}
