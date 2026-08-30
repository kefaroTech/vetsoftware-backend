package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.CreateSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.subscription.application.port.out.CompanyValidationPort;
import com.vetsoftware.app.subscription.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.subscription.application.port.out.PlatformCatalogPort;
import com.vetsoftware.app.subscription.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemCompositionPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionNumberPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionStatusHistoryRepository;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.PlatformCatalogNotConfiguredForSubscriptionException;
import com.vetsoftware.app.subscription.domain.PriceListRef;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemOverlapGuard;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
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
    private final PriceListQueryPort priceListQueryPort;
    private final CatalogItemValidationPort catalogItemValidationPort;
    private final LimitDimensionQueryPort limitDimensionQueryPort;
    private final PlatformCatalogPort platformCatalogPort;
    private final SubscriptionItemCompositionPort compositionPort;
    private final SubscriptionNumberPort subscriptionNumberPort;
    private final SubscriptionChangedPort subscriptionChangedPort;
    private final Clock clock;

    public CreateSubscriptionService(SubscriptionRepository repository,
            SubscriptionItemRepository itemRepository,
            SubscriptionStatusHistoryRepository historyRepository,
            CompanyValidationPort companyValidationPort, PriceListQueryPort priceListQueryPort,
            CatalogItemValidationPort catalogItemValidationPort,
            LimitDimensionQueryPort limitDimensionQueryPort,
            PlatformCatalogPort platformCatalogPort,
            SubscriptionItemCompositionPort compositionPort,
            SubscriptionNumberPort subscriptionNumberPort,
            SubscriptionChangedPort subscriptionChangedPort, Clock clock) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.companyValidationPort = companyValidationPort;
        this.priceListQueryPort = priceListQueryPort;
        this.catalogItemValidationPort = catalogItemValidationPort;
        this.limitDimensionQueryPort = limitDimensionQueryPort;
        this.platformCatalogPort = platformCatalogPort;
        this.compositionPort = compositionPort;
        this.subscriptionNumberPort = subscriptionNumberPort;
        this.subscriptionChangedPort = subscriptionChangedPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionDto execute(CreateSubscriptionCommand command) {
        companyValidationPort.validateExists(command.companyId());
        // D-73 por el lado del contrato. Antes aqui habia un existsById pelado, asi
        // que se podia firmar una cabecera que apuntara a una lista EN BORRADOR o
        // CADUCADA. No se colaba del todo -las lineas fallaban despues contra el
        // catalogo publicado- pero fallaba con el mensaje equivocado: «Published
        // catalog price not found for item» acusa al articulo cuando la culpable es
        // la tarifa, y quien lo leyera se ponia a revisar el catalogo. La cabecera
        // exige lo mismo que las lineas -publicada, y vigente por fecha SALVO cuando
        // la firma viene de una cotizacion aceptada; ver el bloque de abajo- y con el
        // MISMO predicado del kernel -PriceListValidity, que solo sabe de dos fechas-:
        // dos comparaciones que nada obliga a mover juntas es el defecto de manana.
        // Quien pone el id y el codigo en el fallo es el companion VO de esta rodaja,
        // porque el kernel no puede saber que existen las tarifas.
        PriceListRef priceList = priceListQueryPort.findPublishedById(command.priceListId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Published price list not found: " + command.priceListId()));
        // La vigencia POR FECHA solo se exige cuando no hay cotizacion detras, y esa
        // asimetria es deliberada.
        //
        // Una cotizacion tiene su propia vigencia -quince dias en autoservicio- y su
        // propia tarifa, y las dos pueden no durar lo mismo: la lista puede caducar el
        // dia 8 de una oferta que promete precio hasta el 15. Antes eso hacia fallar la
        // firma con «Published price list not found» o con el error de vigencia, es
        // decir acusando a la tarifa de un desfase que el cliente no provoco, no puede
        // entender y no puede resolver: hizo exactamente lo que se le ofrecio, dentro
        // del plazo que se le prometio.
        //
        // Y comprobarla no protegia nada, porque en ese camino LA TARIFA NO SE USA:
        // AcceptedQuoteContractLines copia precio, IVA, descuento y tramo ya congelados
        // en los renglones de la oferta (D-66/D-86) y no vuelve al catalogo ni una vez.
        // Exigir que la lista siga vigente era pedir que estuviera viva algo que no se
        // consulta. La plataforma se obliga por lo que ofrecio; si no quiere sostener
        // un precio quince dias, lo que tiene que acortar es la vigencia de la oferta
        // -que es suya- y no romper la firma al final.
        //
        // Lo que SI se sigue exigiendo en los dos caminos es que la lista este
        // PUBLICADA: un borrador nunca fue una oferta, y una cotizacion emitida contra
        // uno seria un defecto anterior que esto no debe blanquear.
        //
        // La fecha, cuando se comprueba, es hoy EN BOGOTA: sale del reloj inyectado,
        // que es el unico que lleva la zona del negocio (D-81). Un LocalDate.now()
        // pelado ya contesta manana entre las 19:00 y la medianoche y rechazaria un
        // alta legitima el ultimo dia de la tarifa.
        if (command.quoteId() == null) {
            priceList.requireEffectiveOn(LocalDate.now(clock));
        }

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

        // D-76: la composicion se congela AL FIRMAR, en la misma transaccion que la
        // linea. Si esto fallara, el contrato no nace: una linea sin foto es una linea
        // sin permisos, y prefiero que no exista a que exista muda.
        for (SubscriptionItem saved : itemRepository
                .saveAll(buildInitialItems(command, subscription))) {
            compositionPort.freeze(saved.getCompanyId(), saved.getId(), saved.getCatalogItemId());
        }

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
            SubscriptionItemOverlapGuard.ensureNoOverlap(line.catalogItemId(),
                    line.tierMinOrDefault(), period,
                    items.stream().filter(i -> i.getCatalogItemId().equals(line.catalogItemId()))
                            .toList());
            SubscriptionItem item = SubscriptionItem.open(subscription.getCompanyId(),
                    subscription.getId(), line.catalogItemId(), line.itemCode(), line.itemName(),
                    line.itemType(), line.capacityUnit(), line.tierMinOrDefault(), line.tierMax(),
                    line.includedQuantity() == null ? 0 : line.includedQuantity(),
                    line.taxTreatment(), line.quantity() == null ? 1 : line.quantity(),
                    line.unitAmount(), line.discountPercentOrZero(), line.discountAmountOrZero(),
                    line.discountIsConditional(), line.taxRate(), period, ItemOrigin.INITIAL, null);
            // Despues de construir la linea: el dominio decide primero si esa linea
            // puede llevar unidad -una unidad colgada de un MODULE se rechaza por lo
            // que es- y solo entonces se le pregunta al catalogo si el eje existe.
            requireKnownAxis(item.getCapacityUnit());
            items.add(item);
        }
        return items;
    }

    /**
     * El eje que se firma tiene que existir en el catalogo.
     *
     * <p>
     * <strong>Es la unica puerta de entrada por la que un codigo de eje llega sin
     * pasar por el catalogo</strong>, y por eso la comprobacion vive aqui y no en
     * los otros tres caminos de alta: el contrato inicial y la aceptacion de
     * cotizacion resuelven la unidad leyendo {@code catalog_items}, cuya columna ya
     * va atada por clave foranea desde el changeset 333. Estas lineas, en cambio,
     * llegan escritas en el cuerpo de la peticion.
     *
     * <p>
     * <strong>Antes de ese changeset no hacia falta y ahora si.</strong> Mientras
     * el campo era un enumerado de cuatro valores, Jackson rechazaba cualquier otra
     * cosa al deserializar; ahora es una cadena, asi que sin esta consulta un
     * {@code "ANIMALES"} mal escrito llegaria al {@code INSERT} y saldria como un
     * {@code 500} con una violacion de clave foranea que no le dice a nadie que el
     * problema es una letra de mas.
     *
     * <p>
     * Cuesta una consulta por linea de capacidad —el catalogo de ejes son ocho
     * filas— y solo en el alta, no en la lectura.
     */
    private void requireKnownAxis(String capacityUnit) {
        if (capacityUnit == null)
            return;
        if (limitDimensionQueryPort.findByCode(capacityUnit).isEmpty())
            throw new IllegalArgumentException("capacityUnit '" + capacityUnit
                    + "' is not a known limit dimension: seed a limit_dimensions row with that"
                    + " code before contracting capacity of that axis. Codes are case sensitive.");
    }

    private static String actorOf(CreateSubscriptionCommand command) {
        return command.actor() == null || command.actor().isBlank()
                ? SYSTEM_ACTOR
                : command.actor();
    }
}
