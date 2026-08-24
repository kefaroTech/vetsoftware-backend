package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionItemQuantityCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionItemQuantityUseCase;
import com.vetsoftware.app.subscription.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAmendmentRepository;
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
import com.vetsoftware.app.subscription.domain.SubscriptionItemOverlapGuard;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambio de cantidad: <strong>cerrar la linea vigente y abrir otra</strong>.
 *
 * <p>
 * Nunca se edita la fila existente, y hay dos motivos independientes. El
 * primero es el precio: {@code unit_amount} es el precio congelado del cliente
 * y cambiarlo reescribiria lo que costaba el periodo ya devengado. El segundo
 * es {@code included_quantity}, que va congelada al firmar — si lo incluido se
 * releyera de la tarifa, editar un tramo cambiaria retroactivamente cuantos
 * usuarios le sobran a quien firmo hace un ano, que es la causa numero uno de
 * sobrefacturacion en modelos de suscripcion. La sucesora arrastra los dos
 * intactos: lo que se renegocio fue cuantas unidades, no a que precio.
 *
 * <p>
 * El orden importa: primero se cierra la original en la fecha efectiva y
 * despues se abre la sucesora desde esa misma fecha. Con el intervalo
 * semiabierto {@code [from, to)} de {@code EffectivePeriod} las dos no se pisan
 * ni dejan hueco, ni siquiera el dia del cambio.
 */
@Observed(name = "subscription.item.quantity.change")
@Service
public class ChangeSubscriptionItemQuantityService
        implements
            ChangeSubscriptionItemQuantityUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionItemRepository itemRepository;
    private final SubscriptionAmendmentRepository amendmentRepository;
    private final EmployeeQueryPort employeeQueryPort;
    private final SystemUserValidationPort systemUserValidationPort;
    private final SubscriptionNumberPort subscriptionNumberPort;
    private final SubscriptionChangedPort subscriptionChangedPort;

    public ChangeSubscriptionItemQuantityService(SubscriptionRepository subscriptionRepository,
            SubscriptionItemRepository itemRepository,
            SubscriptionAmendmentRepository amendmentRepository,
            EmployeeQueryPort employeeQueryPort, SystemUserValidationPort systemUserValidationPort,
            SubscriptionNumberPort subscriptionNumberPort,
            SubscriptionChangedPort subscriptionChangedPort) {
        this.subscriptionRepository = subscriptionRepository;
        this.itemRepository = itemRepository;
        this.amendmentRepository = amendmentRepository;
        this.employeeQueryPort = employeeQueryPort;
        this.systemUserValidationPort = systemUserValidationPort;
        this.subscriptionNumberPort = subscriptionNumberPort;
        this.subscriptionChangedPort = subscriptionChangedPort;
    }

    @Override
    @Transactional
    public SubscriptionItemDto execute(ChangeSubscriptionItemQuantityCommand command) {
        // Idempotencia: buscar antes de insertar. El reintento devuelve la sucesora que
        // creo el primer intento, no una tercera linea.
        Optional<SubscriptionAmendment> replay = amendmentRepository
                .findByClientRequestIdAndCompanyId(command.clientRequestId(), command.companyId());
        if (replay.isPresent()) {
            Long amendmentId = replay.get().getId();
            return itemRepository
                    .findByCreatedAmendmentIdAndCompanyId(amendmentId, command.companyId())
                    .map(SubscriptionItemDto::from)
                    .orElseThrow(() -> new SubscriptionItemNotFoundException(amendmentId));
        }

        Subscription subscription = subscriptionRepository
                .lockByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.id()));
        SubscriptionItem original = itemRepository
                .findByIdAndCompanyId(command.subscriptionItemId(), command.companyId())
                .orElseThrow(
                        () -> new SubscriptionItemNotFoundException(command.subscriptionItemId()));

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

        // La diferencia entre lo que costara la sucesora y lo que costaba la original,
        // con el precio unitario y lo incluido de la original —que es justo lo que la
        // sucesora arrastra intacto—. Sube o baja segun el signo, sin ninguna rama.
        BigDecimal cycleDelta = SubscriptionItem
                .recurringSubtotalOf(command.newQuantity() == null ? 0 : command.newQuantity(),
                        original.getIncludedQuantity(), original.getUnitAmount())
                .subtract(original.recurringSubtotal());
        Proration proration = ProrationCalculator.onCurrentPeriod(cycleDelta,
                BillingPeriod.of(subscription), EffectivePeriod.openFrom(command.effectiveDate()));

        SubscriptionAmendment amendment = amendmentRepository
                .save(SubscriptionAmendment.issue(command.companyId(), subscription.getId(),
                        subscriptionNumberPort.nextAmendmentNumber(
                                command.effectiveDate().getYear()),
                        AmendmentType.CHANGE_QUANTITY, command.effectiveDate(), command.reason(),
                        command.requestedByEmployeeId(), command.requestedBySystemUserId(),
                        proration.amount(), proration.cycleDeltaAmount(), null,
                        command.clientRequestId()));

        // Cerrar primero: la original deja de estar abierta y libera el
        // current_item_marker, que es lo que permite que la sucesora lo ocupe.
        original.endOn(command.effectiveDate(), amendment.getId());
        itemRepository.save(original);

        SubscriptionItem successor = original.withQuantity(
                command.newQuantity() == null ? 0 : command.newQuantity(), command.effectiveDate(),
                amendment.getId());

        // Aun cerrada la original, otro tramo futuro del mismo articulo si podria
        // pisarse con la sucesora: hay que preguntarlo. Se excluye la original porque
        // ya no puede solaparse consigo misma.
        SubscriptionItemOverlapGuard.ensureNoOverlap(original.getCatalogItemId(),
                successor.getPeriod(),
                itemRepository.findOverlapping(command.companyId(), subscription.getId(),
                        original.getCatalogItemId(), successor.getPeriod().from(),
                        successor.getPeriod().to(), original.getId()));

        SubscriptionItem saved = itemRepository.save(successor);

        subscriptionChangedPort.subscriptionChanged(
                new SubscriptionChangedEvent(command.companyId(), subscription.getId(),
                        SubscriptionChangeKind.QUANTITY_CHANGED, command.effectiveDate()));

        return SubscriptionItemDto.from(saved);
    }
}
