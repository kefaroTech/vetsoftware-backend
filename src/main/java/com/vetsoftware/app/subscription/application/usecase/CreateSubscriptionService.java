package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.CreateSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.subscription.application.port.out.CompanyValidationPort;
import com.vetsoftware.app.subscription.application.port.out.PlatformCatalogPort;
import com.vetsoftware.app.subscription.application.port.out.PriceListValidationPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionNumberPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionStatusHistoryRepository;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.PlatformCatalogNotConfiguredForSubscriptionException;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemOverlapGuard;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta del contrato con sus lineas iniciales, todo en una transaccion (R10:
 * toda empresa nace con un contrato; si algo falla, no nace).
 *
 * <p>
 * <strong>Una empresa, un contrato vivo.</strong> Este servicio NO comprueba
 * antes si ya hay uno: eso seria una carrera —dos altas simultaneas leerian las
 * dos «no hay» e insertarian las dos—. La autoridad es
 * {@code uq_subscriptions_active_company} sobre la columna generada
 * {@code active_marker}, y el adaptador de persistencia traduce su violacion a
 * {@code CompanyAlreadyHasActiveSubscriptionException}, que sale como 409
 * legible.
 */
@Observed(name = "subscription.create")
@Service
public class CreateSubscriptionService implements CreateSubscriptionUseCase {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final SubscriptionRepository repository;
    private final SubscriptionItemRepository itemRepository;
    private final SubscriptionStatusHistoryRepository historyRepository;
    private final CompanyValidationPort companyValidationPort;
    private final PriceListValidationPort priceListValidationPort;
    private final CatalogItemValidationPort catalogItemValidationPort;
    private final PlatformCatalogPort platformCatalogPort;
    private final SubscriptionNumberPort subscriptionNumberPort;
    private final SubscriptionChangedPort subscriptionChangedPort;
    private final Clock clock;

    public CreateSubscriptionService(SubscriptionRepository repository,
            SubscriptionItemRepository itemRepository,
            SubscriptionStatusHistoryRepository historyRepository,
            CompanyValidationPort companyValidationPort,
            PriceListValidationPort priceListValidationPort,
            CatalogItemValidationPort catalogItemValidationPort,
            PlatformCatalogPort platformCatalogPort, SubscriptionNumberPort subscriptionNumberPort,
            SubscriptionChangedPort subscriptionChangedPort, Clock clock) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.companyValidationPort = companyValidationPort;
        this.priceListValidationPort = priceListValidationPort;
        this.catalogItemValidationPort = catalogItemValidationPort;
        this.platformCatalogPort = platformCatalogPort;
        this.subscriptionNumberPort = subscriptionNumberPort;
        this.subscriptionChangedPort = subscriptionChangedPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionDto execute(CreateSubscriptionCommand command) {
        companyValidationPort.validateExists(command.companyId());
        priceListValidationPort.validateExists(command.priceListId());

        // Los dias de gracia por defecto se resuelven AQUI y en ningun otro sitio.
        // Habia dos caminos de alta y solo uno leia la configuracion de plataforma:
        // el otro quemaba un cero, y un contrato con gracia cero pasa a solo lectura
        // el dia siguiente al vencimiento —el motor de mora dispara GRACE_STARTED y
        // READ_ONLY_APPLIED en la misma pasada, y deja escrito en la bitacora
        // «gracia de 0 dias agotada»: el defecto se confesaba en su propio registro
        // de auditoria y nadie lo leia—. Un valor por defecto duplicado en dos
        // servicios vuelve a divergir; por eso el otro camino ya no lo pasa (#467).
        int graceDays = command.graceDays() == null
                ? platformCatalogPort.findDefaultGraceDays()
                        .orElseThrow(() -> new PlatformCatalogNotConfiguredForSubscriptionException(
                                command.companyId()))
                : command.graceDays();

        // El consecutivo se reserva DENTRO de esta transaccion: si el alta falla, el
        // numero vuelve a estar libre y la serie no queda con un hueco.
        Subscription subscription = repository.save(Subscription.create(
                subscriptionNumberPort.nextSubscriptionNumber(command.startDate().getYear()),
                command.companyId(), command.quoteId(), command.priceListId(),
                command.billingCycle(), command.status(), command.startDate(),
                command.trialEndDate(), command.currentPeriodStart(), command.currentPeriodEnd(),
                command.nextBillingDate(), command.commitmentEndDate(), graceDays,
                command.autoRenew() == null || command.autoRenew()));

        itemRepository.saveAll(buildInitialItems(command, subscription));

        // La primera fila de la bitacora lleva fromStatus nulo: el contrato no venia
        // de ningun estado.
        historyRepository.append(SubscriptionStatusChange.record(subscription.getCompanyId(),
                subscription.getId(), null, subscription.getStatus(), "Alta del contrato",
                actorOf(command), LocalDateTime.now(clock)));

        // El contrato cambio: el recalculo de permisos y contadores (R11) cuelga de
        // aqui. Este slice no lo hace, solo lo anuncia.
        subscriptionChangedPort.subscriptionChanged(
                new SubscriptionChangedEvent(subscription.getCompanyId(), subscription.getId(),
                        SubscriptionChangeKind.SUBSCRIPTION_CREATED, subscription.getStartDate()));

        return SubscriptionDto.from(subscription);
    }

    /**
     * Construye las lineas congelando en cada una el precio, el IVA y —sobre todo—
     * lo incluido. Comprueba ademas que el propio lote no traiga dos tramos del
     * mismo articulo que se pisen: un alta con dos lineas solapadas factura ese
     * modulo dos veces desde el primer dia.
     */
    private List<SubscriptionItem> buildInitialItems(CreateSubscriptionCommand command,
            Subscription subscription) {
        List<SubscriptionItem> items = new ArrayList<>();
        if (command.items() == null)
            return items;
        for (SubscriptionItemLineCommand line : command.items()) {
            catalogItemValidationPort.validateExists(line.catalogItemId());
            EffectivePeriod period = new EffectivePeriod(line.effectiveFrom() == null
                    ? subscription.getStartDate()
                    : line.effectiveFrom(), line.effectiveTo());
            SubscriptionItemOverlapGuard.ensureNoOverlap(line.catalogItemId(), period,
                    items.stream().filter(i -> i.getCatalogItemId().equals(line.catalogItemId()))
                            .toList());
            items.add(SubscriptionItem.open(subscription.getCompanyId(), subscription.getId(),
                    line.catalogItemId(), line.itemCode(), line.itemName(), line.itemType(),
                    line.capacityUnit(),
                    line.includedQuantity() == null ? 0 : line.includedQuantity(),
                    line.taxTreatment(), line.quantity() == null ? 1 : line.quantity(),
                    line.unitAmount(), line.taxRate(), period, ItemOrigin.INITIAL, null));
        }
        return items;
    }

    private static String actorOf(CreateSubscriptionCommand command) {
        return command.actor() == null || command.actor().isBlank()
                ? SYSTEM_ACTOR
                : command.actor();
    }
}
