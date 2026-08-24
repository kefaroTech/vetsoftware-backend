package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.AddSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.port.in.AddSubscriptionItemUseCase;
import com.vetsoftware.app.subscription.application.port.out.CatalogItemValidationPort;
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
import com.vetsoftware.app.subscription.domain.ItemOrigin;
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
 * Alta de una linea de contrato. Es el caso de uso donde se cruzan las tres
 * cosas que este modelo tiene que garantizar sin ayuda del motor:
 *
 * <ol>
 * <li><strong>Idempotencia.</strong> Se busca el {@code clientRequestId}
 * <em>antes</em> de insertar y dentro de la transaccion. Un {@code try/catch}
 * de la violacion de unique no sirve: convierte el segundo clic en un 500 en
 * vez de en la misma respuesta que el primero.
 * <li><strong>Bloqueo.</strong> Se toma la fila de {@code subscriptions} con
 * bloqueo pesimista antes de leer nada, que es lo que serializa el
 * leer-y-luego-escribir de la comprobacion de solape.
 * <li><strong>Prorrateo.</strong> Los dos importes del otrosi los calcula
 * {@link ProrationCalculator} contra el periodo de facturacion en curso. Antes
 * llegaban en el cuerpo de la peticion, que es tanto como dejar que el emisor
 * decida cuanto se le cobra.
 * <li><strong>Solape.</strong>
 * {@link SubscriptionItemOverlapGuard#ensureNoOverlap} sobre los tramos del
 * mismo articulo. El indice unico cubre las lineas abiertas; los tramos con
 * fecha de fin futura que se pisan solo los cubre esto.
 * </ol>
 */
@Observed(name = "subscription.item.add")
@Service
public class AddSubscriptionItemService implements AddSubscriptionItemUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionItemRepository itemRepository;
    private final SubscriptionAmendmentRepository amendmentRepository;
    private final CatalogItemValidationPort catalogItemValidationPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final SystemUserValidationPort systemUserValidationPort;
    private final SubscriptionNumberPort subscriptionNumberPort;
    private final SubscriptionChangedPort subscriptionChangedPort;

    public AddSubscriptionItemService(SubscriptionRepository subscriptionRepository,
            SubscriptionItemRepository itemRepository,
            SubscriptionAmendmentRepository amendmentRepository,
            CatalogItemValidationPort catalogItemValidationPort,
            EmployeeQueryPort employeeQueryPort, SystemUserValidationPort systemUserValidationPort,
            SubscriptionNumberPort subscriptionNumberPort,
            SubscriptionChangedPort subscriptionChangedPort) {
        this.subscriptionRepository = subscriptionRepository;
        this.itemRepository = itemRepository;
        this.amendmentRepository = amendmentRepository;
        this.catalogItemValidationPort = catalogItemValidationPort;
        this.employeeQueryPort = employeeQueryPort;
        this.systemUserValidationPort = systemUserValidationPort;
        this.subscriptionNumberPort = subscriptionNumberPort;
        this.subscriptionChangedPort = subscriptionChangedPort;
    }

    @Override
    @Transactional
    public SubscriptionItemDto execute(AddSubscriptionItemCommand command) {
        // (1) Idempotencia: buscar antes de insertar, dentro de la transaccion. Dos
        // clics en «Anadir» no pueden generar dos lineas ni dos cobros; el segundo
        // devuelve el recurso que creo el primero.
        Optional<SubscriptionAmendment> replay = amendmentRepository
                .findByClientRequestIdAndCompanyId(command.clientRequestId(), command.companyId());
        if (replay.isPresent()) {
            Long amendmentId = replay.get().getId();
            return itemRepository
                    .findByCreatedAmendmentIdAndCompanyId(amendmentId, command.companyId())
                    .map(SubscriptionItemDto::from)
                    .orElseThrow(() -> new SubscriptionItemNotFoundException(amendmentId));
        }

        // (2) Bloqueo pesimista sobre el contrato: serializa la comprobacion de solape.
        Subscription subscription = subscriptionRepository
                .lockByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.id()));

        SubscriptionItemLineCommand line = command.line();
        if (line == null)
            throw new IllegalArgumentException("line is required");
        catalogItemValidationPort.validateExists(line.catalogItemId());
        // R14: el empleado que firma el otrosi tiene que ser de la misma empresa que el
        // contrato. La FK es simple, asi que la base no puede imponerlo: se resuelve
        // por la variante acotada del puerto, que es la unica que existe.
        if (command.requestedByEmployeeId() != null) {
            employeeQueryPort
                    .findByIdAndCompanyId(command.requestedByEmployeeId(), command.companyId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Employee not found in company: " + command.requestedByEmployeeId()));
        }
        if (command.requestedBySystemUserId() != null) {
            systemUserValidationPort.validateExists(command.requestedBySystemUserId());
        }

        EffectivePeriod period = new EffectivePeriod(
                line.effectiveFrom() == null ? command.effectiveDate() : line.effectiveFrom(),
                line.effectiveTo());

        // (3) Solape: lo que el esquema no puede garantizar.
        SubscriptionItemOverlapGuard.ensureNoOverlap(line.catalogItemId(), period,
                itemRepository.findOverlapping(command.companyId(), subscription.getId(),
                        line.catalogItemId(), period.from(), period.to(), null));

        // (4) Prorrateo: lo calcula el servidor, nunca el cuerpo de la peticion. La
        // linea todavia no existe —necesita el id del otrosi— asi que la cuota que
        // aporta se calcula con la sobrecarga estatica, sobre los mismos numeros con
        // los que se va a abrir.
        BigDecimal cycleDelta = SubscriptionItem.recurringSubtotalOf(
                line.quantity() == null ? 1 : line.quantity(),
                line.includedQuantity() == null ? 0 : line.includedQuantity(), line.unitAmount());
        // La ventana es el tramo de la propia linea, no la fecha del otrosi: un alta
        // puede traer su effectiveFrom, y hasta su effectiveTo, y prorratear por la
        // fecha del papel cobraria dias que la linea no sirve.
        Proration proration = ProrationCalculator.onCurrentPeriod(cycleDelta,
                BillingPeriod.of(subscription), period);

        SubscriptionAmendment amendment = amendmentRepository
                .save(SubscriptionAmendment.issue(command.companyId(), subscription.getId(),
                        subscriptionNumberPort
                                .nextAmendmentNumber(command.effectiveDate().getYear()),
                        AmendmentType.ADD_ITEM, command.effectiveDate(), command.reason(),
                        command.requestedByEmployeeId(), command.requestedBySystemUserId(),
                        proration.amount(), proration.cycleDeltaAmount(), command.quoteId(),
                        command.clientRequestId()));

        SubscriptionItem saved = itemRepository.save(SubscriptionItem.open(command.companyId(),
                subscription.getId(), line.catalogItemId(), line.itemCode(), line.itemName(),
                line.itemType(), line.capacityUnit(),
                line.includedQuantity() == null ? 0 : line.includedQuantity(), line.taxTreatment(),
                line.quantity() == null ? 1 : line.quantity(), line.unitAmount(), line.taxRate(),
                period, ItemOrigin.ADDON, amendment.getId()));

        subscriptionChangedPort.subscriptionChanged(
                new SubscriptionChangedEvent(command.companyId(), subscription.getId(),
                        SubscriptionChangeKind.ITEM_ADDED, command.effectiveDate()));

        return SubscriptionItemDto.from(saved);
    }
}
