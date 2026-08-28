package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.RemoveSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.port.in.RemoveSubscriptionItemUseCase;
import com.vetsoftware.app.subscription.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAmendmentRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAuditPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionNumberPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.subscription.domain.AmendmentType;
import com.vetsoftware.app.subscription.domain.BillingPeriod;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.Proration;
import com.vetsoftware.app.subscription.domain.ProrationCalculator;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionAmendment;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemNotFoundException;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baja de una linea del contrato.
 *
 * <p>
 * <strong>Dar de baja no borra</strong> (R12). Este servicio escribe
 * {@code effective_to} y el otrosi que la cerro, y no toca nada mas: ni elimina
 * la fila, ni la desactiva, ni roza una sola tabla clinica o comercial. La
 * informacion de que ese cliente tuvo ese modulo entre marzo y septiembre es
 * legalmente suya y no tiene vuelta atras si se destruye. Lo que baja es el
 * nivel de acceso, y eso lo decide el recalculo de permisos —que se dispara con
 * el evento de abajo—, no este caso de uso.
 */
@Observed(name = "subscription.item.remove")
@Service
public class RemoveSubscriptionItemService implements RemoveSubscriptionItemUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionItemRepository itemRepository;
    private final SubscriptionAmendmentRepository amendmentRepository;
    private final EmployeeQueryPort employeeQueryPort;
    private final SystemUserValidationPort systemUserValidationPort;
    private final SubscriptionNumberPort subscriptionNumberPort;
    private final SubscriptionChangedPort subscriptionChangedPort;
    private final SubscriptionAuditPort audit;

    public RemoveSubscriptionItemService(SubscriptionRepository subscriptionRepository,
            SubscriptionItemRepository itemRepository,
            SubscriptionAmendmentRepository amendmentRepository,
            EmployeeQueryPort employeeQueryPort, SystemUserValidationPort systemUserValidationPort,
            SubscriptionNumberPort subscriptionNumberPort,
            SubscriptionChangedPort subscriptionChangedPort, SubscriptionAuditPort audit) {
        this.subscriptionRepository = subscriptionRepository;
        this.itemRepository = itemRepository;
        this.amendmentRepository = amendmentRepository;
        this.employeeQueryPort = employeeQueryPort;
        this.systemUserValidationPort = systemUserValidationPort;
        this.subscriptionNumberPort = subscriptionNumberPort;
        this.subscriptionChangedPort = subscriptionChangedPort;
        this.audit = audit;
    }

    @Override
    @Transactional
    public SubscriptionItemDto execute(RemoveSubscriptionItemCommand command) {
        // Idempotencia: buscar antes de insertar. El reintento devuelve la linea ya
        // cerrada, no un error.
        Optional<SubscriptionAmendment> replay = amendmentRepository
                .findByClientRequestIdAndCompanyId(command.clientRequestId(), command.companyId());
        if (replay.isPresent()) {
            return SubscriptionItemDto.from(loadItem(command));
        }

        Subscription subscription = subscriptionRepository
                .lockByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.id()));
        SubscriptionItem item = loadItem(command);

        // R14: el empleado que firma es de la misma empresa que el contrato.
        if (command.requestedByEmployeeId() != null) {
            employeeQueryPort
                    .findByIdAndCompanyId(command.requestedByEmployeeId(), command.companyId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Employee not found in company: " + command.requestedByEmployeeId()));
        }
        if (command.requestedBySystemUserId() != null) {
            systemUserValidationPort.validateExists(command.requestedBySystemUserId());
        }

        // El abono, calculado por el servidor con el precio congelado en la propia
        // fila. Negativo: la baja resta, y son los mismos dias que habria cobrado un
        // alta de la misma fecha, de modo que alta y baja del mismo dia suman cero.
        Proration proration = ProrationCalculator.onCurrentPeriod(item.recurringSubtotal().negate(),
                BillingPeriod.of(subscription), EffectivePeriod.openFrom(command.effectiveDate()));

        SubscriptionAmendment amendment = amendmentRepository
                .save(SubscriptionAmendment.issue(command.companyId(), subscription.getId(),
                        subscriptionNumberPort.nextAmendmentNumber(
                                command.effectiveDate().getYear()),
                        AmendmentType.REMOVE_ITEM, command.effectiveDate(), command.reason(),
                        command.requestedByEmployeeId(), command.requestedBySystemUserId(),
                        proration.amount(), proration.cycleDeltaAmount(), null,
                        command.clientRequestId()));

        // Poner la fecha de fin. Nada mas.
        item.endOn(command.effectiveDate(), amendment.getId());
        SubscriptionItem saved = itemRepository.save(item);

        // «Baja de modulo» dejaba de rastro un http_mutation que no decia QUE modulo.
        audit.itemRemoved(subscription.getId(), saved.getId(), proration.cycleDeltaAmount(),
                amendment.getId());

        subscriptionChangedPort.subscriptionChanged(
                new SubscriptionChangedEvent(command.companyId(), subscription.getId(),
                        SubscriptionChangeKind.ITEM_REMOVED, command.effectiveDate()));

        return SubscriptionItemDto.from(saved);
    }

    private SubscriptionItem loadItem(RemoveSubscriptionItemCommand command) {
        return itemRepository
                .findByIdAndCompanyId(command.subscriptionItemId(), command.companyId())
                .orElseThrow(
                        () -> new SubscriptionItemNotFoundException(command.subscriptionItemId()));
    }
}
