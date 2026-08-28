package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.AddSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.command.RequestedSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.dto.PublishedCatalogItem;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.port.in.AddSubscriptionItemUseCase;
import com.vetsoftware.app.subscription.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAmendmentRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAuditPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionCommercialSnapshotPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemCompositionPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionNumberPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.subscription.domain.AmendmentType;
import com.vetsoftware.app.subscription.domain.BillingPeriod;
import com.vetsoftware.app.subscription.domain.ContractPriceTiers;
import com.vetsoftware.app.subscription.domain.ContractTierLine;
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
import com.vetsoftware.app.shared.domain.Money;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta de una linea de contrato. Es el caso de uso donde se cruzan las cuatro
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
 *
 * <p>
 * <strong>Y el precio se resuelve AQUI, no llega del cuerpo
 * (R-QUOTE-02).</strong> Esta es la correccion que cierra el defecto construido
 * #2. La peticion traia {@code unitAmount}, {@code itemName}, {@code itemType},
 * {@code capacityUnit}, {@code includedQuantity} y {@code taxRate}, y este
 * servicio los copiaba tal cual a la fila con una sola comprobacion —que el
 * articulo existiera—. Se podia abrir una linea <em>a cero pesos</em>, con
 * nombre inventado, o con nueve mil novecientas noventa y nueve unidades
 * incluidas que iban directas al techo del contador sin pasar por ninguna
 * tarifa. Y el prorrateo «que calcula el servidor» se calculaba sobre ese
 * importe del cuerpo, asi que la proteccion que su comentario reclamaba era
 * hueca.
 *
 * <p>
 * El patron es el de {@code CreateRequestedSubscriptionService}: la seleccion
 * comercial —articulo, cantidad y fechas— es lo unico que viaja, y el resto
 * sale de la tarifa <strong>del propio contrato</strong>, comprobando que sigue
 * publicada y vigente por fecha (D-73). Con el precio ya fuera del cuerpo, el
 * motivo por el que este puerto seguia cerrado al tenant desaparece.
 */
@Observed(name = "subscription.item.add")
@Service
public class AddSubscriptionItemService implements AddSubscriptionItemUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionItemRepository itemRepository;
    private final SubscriptionAmendmentRepository amendmentRepository;
    private final SubscriptionCommercialSnapshotPort commercialSnapshotPort;
    private final SubscriptionItemCompositionPort compositionPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final SystemUserValidationPort systemUserValidationPort;
    private final SubscriptionNumberPort subscriptionNumberPort;
    private final SubscriptionChangedPort subscriptionChangedPort;
    private final SubscriptionAuditPort audit;

    public AddSubscriptionItemService(SubscriptionRepository subscriptionRepository,
            SubscriptionItemRepository itemRepository,
            SubscriptionAmendmentRepository amendmentRepository,
            SubscriptionCommercialSnapshotPort commercialSnapshotPort,
            SubscriptionItemCompositionPort compositionPort, EmployeeQueryPort employeeQueryPort,
            SystemUserValidationPort systemUserValidationPort,
            SubscriptionNumberPort subscriptionNumberPort,
            SubscriptionChangedPort subscriptionChangedPort, SubscriptionAuditPort audit) {
        this.subscriptionRepository = subscriptionRepository;
        this.itemRepository = itemRepository;
        this.amendmentRepository = amendmentRepository;
        this.commercialSnapshotPort = commercialSnapshotPort;
        this.compositionPort = compositionPort;
        this.employeeQueryPort = employeeQueryPort;
        this.systemUserValidationPort = systemUserValidationPort;
        this.subscriptionNumberPort = subscriptionNumberPort;
        this.subscriptionChangedPort = subscriptionChangedPort;
        this.audit = audit;
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
                    .findAllByCreatedAmendmentIdAndCompanyId(amendmentId, command.companyId())
                    .stream().findFirst().map(SubscriptionItemDto::from)
                    .orElseThrow(() -> new SubscriptionItemNotFoundException(amendmentId));
        }

        // (2) Bloqueo pesimista sobre el contrato: serializa la comprobacion de solape.
        Subscription subscription = subscriptionRepository
                .lockByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.id()));

        RequestedSubscriptionItemCommand line = command.line();
        if (line == null)
            throw new IllegalArgumentException("line is required");
        if (line.catalogItemId() == null)
            throw new IllegalArgumentException("catalogItemId is required");
        int quantity = line.quantity() == null ? 1 : line.quantity();
        if (quantity < 1)
            throw new IllegalArgumentException("quantity must be greater than zero");
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

        // (3) El precio, el nombre, el tipo, la unidad, el IVA y lo incluido salen de
        // la
        // tarifa DEL CONTRATO —la que se firmo, no una que elija quien llama— y solo si
        // sigue publicada y vigente el dia en que la linea empieza a servir (D-73).
        PublishedCatalogItem published = commercialSnapshotPort
                .findPublishedItem(subscription.getPriceListId(), subscription.getBillingCycle(),
                        line.catalogItemId(), quantity, period.from())
                .orElseThrow(
                        () -> new IllegalArgumentException("No published price for catalog item "
                                + line.catalogItemId() + " in the price list of subscription "
                                + subscription.getId() + " on " + period.from()));
        // D-66: los tramos son acumulativos, asi que una ampliacion escalonada abre una
        // linea por tramo, cada una a su precio. Trece unidades extra son ocho a 12.000
        // y cinco a 9.000; cobrarlas todas al tramo alto es el defecto que D-66 cierra.
        List<ContractTierLine> allocation = ContractPriceTiers.allocate(quantity,
                published.tiers());

        // (4) Solape: lo que el esquema no puede garantizar. Se comprueba TRAMO A
        // TRAMO,
        // igual que uq_subscription_items_current, porque dos lineas del mismo articulo
        // en tramos distintos son legitimas y no un cobro doble.
        for (ContractTierLine tierLine : allocation) {
            SubscriptionItemOverlapGuard.ensureNoOverlap(line.catalogItemId(),
                    tierLine.tier().tierMin(), period,
                    itemRepository.findOverlapping(command.companyId(), subscription.getId(),
                            line.catalogItemId(), period.from(), period.to(), null));
        }

        // (5) Prorrateo: lo calcula el servidor sobre el precio de la TARIFA, nunca
        // sobre un importe del cuerpo. La linea todavia no existe —necesita el id del
        // otrosi— asi que la cuota que aporta se calcula con la sobrecarga estatica,
        // sobre los mismos numeros con los que se va a abrir, y sumando todos los
        // tramos: la ampliacion sube la cuota lo que suman sus lineas.
        BigDecimal cycleDelta = Money.zero();
        for (ContractTierLine tierLine : allocation) {
            cycleDelta = cycleDelta.add(SubscriptionItem.recurringSubtotalOf(tierLine.quantity(),
                    tierLine.includedQuantity(), tierLine.tier().unitAmount()));
        }
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

        List<SubscriptionItem> opened = new ArrayList<>();
        for (ContractTierLine tierLine : allocation) {
            // D-76: la composicion se congela al firmar, tambien en una ampliacion.
            opened.add(itemRepository.save(SubscriptionItem.open(command.companyId(),
                    subscription.getId(), published.catalogItemId(), published.itemCode(),
                    published.itemName(), published.itemType(), published.capacityUnit(),
                    tierLine.tier().tierMin(), tierLine.tier().tierMax(),
                    tierLine.includedQuantity(), tierLine.tier().taxTreatment(),
                    tierLine.quantity(), tierLine.tier().unitAmount(), Money.zero(), Money.zero(),
                    false, tierLine.tier().taxRate(), period, ItemOrigin.ADDON,
                    amendment.getId())));
        }
        for (SubscriptionItem saved : opened) {
            compositionPort.freeze(saved.getCompanyId(), saved.getId(), saved.getCatalogItemId());
        }
        SubscriptionItem first = opened.get(0);

        // cycleDeltaAmount es lo que sube la cuota recurrente, y es EL campo del
        // issue #607: el unico dato que prueba que importe se le mostro al cliente
        // antes de confirmar, cuando niegue haber aceptado la ampliacion.
        audit.itemAdded(subscription.getId(), first.getId(), first.getCatalogItemId(), quantity,
                proration.cycleDeltaAmount(), amendment.getId());

        subscriptionChangedPort.subscriptionChanged(
                new SubscriptionChangedEvent(command.companyId(), subscription.getId(),
                        SubscriptionChangeKind.ITEM_ADDED, command.effectiveDate()));

        return SubscriptionItemDto.from(first);
    }
}
