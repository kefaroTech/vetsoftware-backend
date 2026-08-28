package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.CancelSubscriptionCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.CancelSubscriptionUseCase;
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
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancelacion, que son <strong>dos fechas y no una</strong>.
 *
 * <p>
 * {@code cancel_requested_at} es cuando lo pidio y
 * {@code cancel_effective_date} es cuando surte efecto: el cliente cancela el
 * 10 y se va el 30, que es lo que ya pago. Este servicio anota la peticion y
 * <strong>no cambia el estado</strong>: hasta el 30 el contrato sigue siendo el
 * vigente de su empresa, sigue ocupando {@code active_marker} y el cliente
 * sigue trabajando con normalidad. El paso a {@code CANCELLED} lo hace el
 * proceso que atiende la fecha efectiva, por
 * {@code ChangeSubscriptionStatusUseCase}.
 *
 * <p>
 * {@code cancel_reason} se guarda porque es informacion de negocio: es la unica
 * fuente que dice por que se van los clientes.
 *
 * <p>
 * <strong>El otrosi de baja lleva su prorrateo calculado aqui.</strong> Si la
 * fecha efectiva cae dentro del periodo ya facturado, los dias que el cliente
 * pago y no va a consumir se abonan en negativo; si cae en el ultimo dia del
 * periodo o despues —el caso normal, «me voy cuando se acabe lo que pague»— la
 * fraccion es cero o de un dia y el importe sale en consecuencia. El dinero es
 * append-only: esto emite el documento, no edita ninguno.
 */
@Observed(name = "subscription.cancel")
@Service
public class CancelSubscriptionService implements CancelSubscriptionUseCase {

    private final SubscriptionRepository repository;
    private final SubscriptionItemRepository itemRepository;
    private final SubscriptionAmendmentRepository amendmentRepository;
    private final EmployeeQueryPort employeeQueryPort;
    private final SystemUserValidationPort systemUserValidationPort;
    private final SubscriptionNumberPort subscriptionNumberPort;
    private final SubscriptionChangedPort subscriptionChangedPort;
    private final SubscriptionAuditPort audit;

    public CancelSubscriptionService(SubscriptionRepository repository,
            SubscriptionItemRepository itemRepository,
            SubscriptionAmendmentRepository amendmentRepository,
            EmployeeQueryPort employeeQueryPort, SystemUserValidationPort systemUserValidationPort,
            SubscriptionNumberPort subscriptionNumberPort,
            SubscriptionChangedPort subscriptionChangedPort, SubscriptionAuditPort audit) {
        this.repository = repository;
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
    public SubscriptionDto execute(CancelSubscriptionCommand command) {
        // Idempotencia: dos clics en «Cancelar» no emiten dos otrosies de baja.
        Optional<SubscriptionAmendment> replay = amendmentRepository
                .findByClientRequestIdAndCompanyId(command.clientRequestId(), command.companyId());
        if (replay.isPresent()) {
            return SubscriptionDto.from(load(command));
        }

        Subscription subscription = load(command);

        // R14.
        if (command.requestedByEmployeeId() != null) {
            employeeQueryPort
                    .findByIdAndCompanyId(command.requestedByEmployeeId(), command.companyId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Employee not found in company: " + command.requestedByEmployeeId()));
        }
        if (command.requestedBySystemUserId() != null) {
            systemUserValidationPort.validateExists(command.requestedBySystemUserId());
        }

        // El abono por lo que el cliente pago y ya no va a consumir, y la caida de la
        // cuota recurrente: la suma de todas las lineas que seguirian vigentes ese dia,
        // en negativo. Se lee sin paginar a proposito —ver findAllCurrentOn—: sumar
        // sobre una pagina daria un abono corto y en silencio.
        BigDecimal cycleDelta = itemRepository
                .findAllCurrentOn(subscription.getId(), command.companyId(),
                        command.effectiveDate())
                .stream().map(SubscriptionItem::recurringSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).negate();
        Proration proration = ProrationCalculator.onCurrentPeriod(cycleDelta,
                BillingPeriod.of(subscription), EffectivePeriod.openFrom(command.effectiveDate()));

        subscription.requestCancellation(command.requestedAt(), command.effectiveDate(),
                command.reason());

        amendmentRepository
                .save(SubscriptionAmendment.issue(command.companyId(), subscription.getId(),
                        subscriptionNumberPort.nextAmendmentNumber(
                                command.effectiveDate().getYear()),
                        AmendmentType.CANCEL, command.effectiveDate(), command.reason(),
                        command.requestedByEmployeeId(), command.requestedBySystemUserId(),
                        proration.amount(), proration.cycleDeltaAmount(), null,
                        command.clientRequestId()));

        Subscription saved = repository.save(subscription);

        // La solicitud se registra aparte del cambio de estado porque ocurre antes: el
        // contrato sigue vigente hasta effectiveDate, y la distancia entre las dos
        // fechas es justo lo que se discute cuando el cliente reclama el ultimo mes.
        audit.cancellationRequested(saved.getId(), command.effectiveDate());

        subscriptionChangedPort.subscriptionChanged(
                new SubscriptionChangedEvent(saved.getCompanyId(), saved.getId(),
                        SubscriptionChangeKind.CANCELLATION_REQUESTED, command.effectiveDate()));

        return SubscriptionDto.from(saved);
    }

    private Subscription load(CancelSubscriptionCommand command) {
        return repository.findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.id()));
    }
}
