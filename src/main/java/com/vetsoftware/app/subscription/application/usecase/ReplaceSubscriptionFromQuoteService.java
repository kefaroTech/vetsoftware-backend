package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.ReplaceSubscriptionFromQuoteCommand;
import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import com.vetsoftware.app.subscription.application.dto.InitialContractTemplate;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionQuoteSnapshot;
import com.vetsoftware.app.subscription.application.port.in.CreateSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.ReplaceSubscriptionFromQuoteUseCase;
import com.vetsoftware.app.subscription.application.port.out.PlatformCatalogPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAuditPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionLifecycleMetrics;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionQuoteSnapshotPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionStatusHistoryRepository;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.StructuralCapacityMinimum;
import com.vetsoftware.app.subscription.domain.StructuralMinimumNotCarriedException;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChangeReason;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra el contrato vigente y abre el que describe la cotizacion aceptada, en
 * una sola transaccion (DC-2). Ver {@link ReplaceSubscriptionFromQuoteUseCase}
 * para el porque de sustituir en vez de ampliar.
 *
 * <p>
 * <strong>&#9940; El cierre NO pasa por
 * {@code ChangeSubscriptionStatusUseCase}, y esa es la decision menos evidente
 * de esta clase.</strong> Aquel servicio anuncia
 * {@code subscriptionChangedPort.subscriptionChanged(...)} al terminar, y ese
 * anuncio es <em>sincrono y dentro de la misma transaccion</em>
 * ({@code EntitlementRecalculationAdapter} es {@code @Transactional(REQUIRED)}
 * a proposito). El recalculo que dispara resuelve el contrato vigente con
 * {@code findCurrentContractByCompanyId} y <strong>lanza
 * {@code CompanyWithoutContractException} si no hay ninguno</strong>
 * ({@code CompanyEntitlementRecalculator}). Entre el cierre y la apertura no
 * hay ninguno: usarlo aqui haria que la sustitucion reventara <em>siempre</em>,
 * y con un error que acusa a los permisos en vez de al orden de las
 * operaciones.
 *
 * <p>
 * Cerrar por el dominio y anotar la bitacora a mano evita ese anuncio
 * intermedio, y ademas es lo correcto: <strong>para los permisos no hay dos
 * cambios, hay uno</strong>. La empresa pasa del contrato A al B sin ningun
 * instante sin contrato, y el unico recalculo es el que anuncia el alta del
 * nuevo. La metrica y el evento de auditoria del cierre <em>si</em> se emiten,
 * por sus propios puertos, para no perder el rastro que
 * {@code ChangeSubscriptionStatusService} habria dejado.
 *
 * <p>
 * <strong>Por que el orden cierre-antes-que-apertura funciona.</strong>
 * {@code uq_subscriptions_active_company} no admite dos contratos vigentes, y
 * la cola de acciones de Hibernate ejecuta los {@code INSERT} antes que los
 * {@code UPDATE} — el orden natural haria saltar el unique con las dos
 * operaciones correctas. No pasa porque {@code JpaSubscriptionRepository.save}
 * hace {@code saveAndFlush}: el {@code UPDATE} del cierre llega a la base antes
 * de que se intente el {@code INSERT} del alta.
 *
 * <p>
 * <strong>&#9940; AQUI ENTRARA EL PRORRATEO DEL TRAMO NO CONSUMIDO, y hoy no
 * existe a proposito.</strong> Cuando el contrato que se cierra haya sido
 * cobrado de verdad, al cliente le quedara periodo pagado sin usar y habra que
 * abonarselo: el importe se calcula con {@code ProrationCalculator} —que ya
 * vive en este dominio y ya sabe repartir un periodo por dias— sobre
 * {@code getCurrentPeriodStart()}, {@code getCurrentPeriodEnd()} y la suma de
 * {@code SubscriptionItem.recurringSubtotal()} de las lineas vigentes, y el
 * abono se emite por {@code subscriptionbilling} / {@code customercredit}, que
 * es donde vive el dinero. <strong>Hoy no hay nada que devolver</strong>: no
 * hay pasarela, no se ha cobrado un solo peso, y un abono por un cobro que
 * nunca ocurrio seria dinero inventado en la contabilidad. El sitio exacto
 * donde va es {@link #terminate}, justo antes de escribir el cierre, que es
 * cuando todavia se conocen el periodo y las lineas del contrato que muere.
 */
@Observed(name = "subscription.replace.from.quote")
@Service
public class ReplaceSubscriptionFromQuoteService implements ReplaceSubscriptionFromQuoteUseCase {

    /**
     * Quien firma el cierre en la bitacora. No es una persona de plataforma ni el
     * cliente: es la aceptacion misma, y por eso tiene nombre propio en vez de
     * reutilizar el {@code SYSTEM} generico.
     */
    private static final String ACTOR = "quote-acceptance";

    private static final SubscriptionStatusChangeReason REASON = SubscriptionStatusChangeReason.REPLACED_BY_NEW_CONTRACT;

    private final SubscriptionQuoteSnapshotPort quoteSnapshotPort;
    private final SubscriptionRepository repository;
    private final SubscriptionItemRepository itemRepository;
    private final SubscriptionStatusHistoryRepository historyRepository;
    private final SubscriptionAuditPort audit;
    private final SubscriptionLifecycleMetrics metrics;
    private final PlatformCatalogPort platformCatalogPort;
    private final CreateSubscriptionUseCase createSubscriptionUseCase;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public ReplaceSubscriptionFromQuoteService(SubscriptionQuoteSnapshotPort quoteSnapshotPort,
            SubscriptionRepository repository, SubscriptionItemRepository itemRepository,
            SubscriptionStatusHistoryRepository historyRepository, SubscriptionAuditPort audit,
            SubscriptionLifecycleMetrics metrics, PlatformCatalogPort platformCatalogPort,
            CreateSubscriptionUseCase createSubscriptionUseCase, Clock clock) {
        this.quoteSnapshotPort = quoteSnapshotPort;
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.audit = audit;
        this.metrics = metrics;
        this.platformCatalogPort = platformCatalogPort;
        this.createSubscriptionUseCase = createSubscriptionUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionDto execute(ReplaceSubscriptionFromQuoteCommand command) {
        if (command.quoteId() == null || command.companyId() == null)
            throw new IllegalArgumentException("quoteId and companyId are required");
        // El dia sale del reloj inyectado, que es el unico que lleva la zona del
        // negocio (D-81): un LocalDate.now() pelado contesta manana entre las 19:00 y
        // la medianoche y fecharia el contrato un dia despues de firmarse.
        LocalDate today = LocalDate.now(clock);

        SubscriptionQuoteSnapshot quote = quoteSnapshotPort
                .findByIdAndCompanyId(command.quoteId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Quote not found for company: " + command.quoteId()));
        // Los terminos salen de la oferta y de ningun otro sitio, asi que la tarifa y
        // el ciclo esperados son los suyos: la comprobacion cruzada de
        // AcceptedQuoteContractLines sigue existiendo para el camino de consola, que
        // es donde si los pone quien llama.
        ResolvedContractLines resolved = AcceptedQuoteContractLines.from(quote, quote.priceListId(),
                quote.billingCycle());

        Optional<Subscription> replaced = repository.findCurrentByCompanyId(command.companyId());

        // Reintento: si el contrato vigente YA nacio de esta misma cotizacion, se
        // devuelve el que hay y no se firma otro. Sin esta guarda, un segundo clic
        // -o el reintento de un cliente HTTP- cancelaria el contrato recien creado y
        // abriria un tercero a partir del mismo papel, dejando una cadena de
        // contratos cancelados que nadie pidio.
        //
        // &#9940; Esto NO es la garantia, es la comodidad. Un SELECT seguido de un
        // INSERT es una carrera: dos peticiones simultaneas leen las dos «todavia no»
        // y firman las dos. La autoridad tiene que ser un unico de base de datos sobre
        // subscriptions.quote_id -pedido y todavia no aplicado-, exactamente por el
        // mismo razonamiento con el que
        // CompanyAlreadyHasActiveSubscriptionException documenta que el codigo no
        // puede comprobar antes «una empresa, un contrato» y darlo por bueno. Mientras
        // ese indice no exista, la carrera sigue abierta.
        if (replaced.isPresent() && command.quoteId().equals(replaced.get().getQuoteId())) {
            return SubscriptionDto.from(replaced.get());
        }

        // Se calcula ANTES de cerrar: lo que se arrastra sale del contrato que muere.
        List<SubscriptionItemLineCommand> lines = withStructuralMinimum(command.companyId(),
                resolved.items(), replaced.orElse(null), today);

        replaced.ifPresent(previous -> terminate(previous, command.companyId()));

        return createSubscriptionUseCase
                .execute(newContract(command.companyId(), quote, resolved.actor(), lines, today));
    }

    /**
     * <strong>El minimo estructural viaja al contrato nuevo.</strong> Sustituir es,
     * mirado desde las capacidades, borrar unas lineas y escribir otras: una
     * cotizacion de modulos no tiene por que incluir {@code BRANCH} ni
     * {@code USER}, y firmarla tal cual dejaria a la empresa sin poder usar la sede
     * que ya tiene. Lo que falte en la oferta se copia del contrato que se cierra.
     *
     * <p>
     * <strong>Se copian los valores congelados, no la tarifa de hoy.</strong>
     * Volver al catalogo a poner precio a una capacidad que el cliente ya tenia
     * seria recotizarle por sorpresa algo que no pidio: podria salirle mas caro por
     * comprar. Se arrastran precio, IVA, descuento, lo incluido y el tramo tal como
     * estaban.
     *
     * <p>
     * <strong>Y se arrastran TODOS los tramos de ese eje, no solo el
     * primero.</strong> Una capacidad escalonada vive en varias lineas —una por
     * tramo (D-66)— y quedarse con la de entrada le recortaria la cantidad
     * contratada en silencio, que es la peor forma de perderla: el contrato
     * seguiria pareciendo completo.
     */
    private List<SubscriptionItemLineCommand> withStructuralMinimum(Long companyId,
            List<SubscriptionItemLineCommand> signed, Subscription replaced, LocalDate today) {
        Set<String> granted = signed.stream().map(SubscriptionItemLineCommand::capacityUnit)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> missing = StructuralCapacityMinimum.missingFrom(granted);
        if (missing.isEmpty())
            return signed;
        if (replaced == null)
            throw new StructuralMinimumNotCarriedException(companyId, missing);

        List<SubscriptionItem> previous = itemRepository.findAllCurrentOn(replaced.getId(),
                companyId, today);
        List<SubscriptionItemLineCommand> lines = new ArrayList<>(signed);
        Set<String> stillMissing = new LinkedHashSet<>();
        for (String unit : missing) {
            List<SubscriptionItem> sources = previous.stream()
                    .filter(item -> unit.equals(item.getCapacityUnit()))
                    .sorted(Comparator.comparingInt(SubscriptionItem::getTierMin)).toList();
            if (sources.isEmpty()) {
                stillMissing.add(unit);
                continue;
            }
            sources.forEach(item -> lines.add(carryOver(item, today)));
        }
        // Se acumulan y se denuncian TODAS: fallar en la primera obligaria a
        // reintentar para descubrir la segunda, y quien siembra el catalogo necesita
        // la lista entera de una vez. Mismo criterio que requireOperableMinimum.
        if (!stillMissing.isEmpty())
            throw new StructuralMinimumNotCarriedException(companyId, stillMissing);
        return List.copyOf(lines);
    }

    /**
     * La copia de una linea de capacidad al contrato nuevo. Ni un solo recalculo.
     */
    private static SubscriptionItemLineCommand carryOver(SubscriptionItem item, LocalDate from) {
        return new SubscriptionItemLineCommand(item.getCatalogItemId(), item.getItemCode(),
                item.getItemName(), item.getItemType(), item.getCapacityUnit(), item.getTierMin(),
                item.getTierMax(), item.getIncludedQuantity(), item.getTaxTreatment(),
                item.getQuantity(), item.getUnitAmount(), item.getDiscountPercent(),
                item.getDiscountAmount(), item.isDiscountConditional(), item.getTaxRate(), from,
                null);
    }

    /**
     * Cierra el contrato anterior. Ver el javadoc de la clase: por el dominio y no
     * por {@code ChangeSubscriptionStatusUseCase}, para no disparar un recalculo de
     * permisos en el instante en que la empresa no tiene contrato vigente.
     *
     * <p>
     * &#9940; <strong>El prorrateo del tramo no consumido va aqui</strong> cuando
     * exista cobro real. Ver el javadoc de la clase.
     */
    private void terminate(Subscription replaced, Long companyId) {
        LocalDateTime occurredAt = LocalDateTime.now(clock);
        SubscriptionStatusChange change = replaced.changeStatus(SubscriptionStatus.CANCELLED,
                REASON.code(), ACTOR, occurredAt);
        Subscription saved = repository.save(replaced);
        historyRepository.append(new SubscriptionStatusChange(null, companyId, saved.getId(),
                change.getFromStatus(), change.getToStatus(), change.getReason(),
                change.getOccurredAt(), change.getActor(), null));
        metrics.statusTransitioned(change.getToStatus());
        audit.statusChanged(saved.getId(), change.getFromStatus(), change.getToStatus(), REASON);
    }

    /**
     * La cabecera del contrato nuevo.
     *
     * <p>
     * <strong>Nace sin cobrar, y en que estado nace lo decide el catalogo de
     * plataforma</strong>, no esta clase: exactamente la misma regla que aplica
     * {@code CreateInitialSubscriptionService} al contrato con el que nace una
     * empresa —{@code TRIALING} si hay prueba por defecto, {@code ACTIVE} si no—.
     * Inventar aqui una ventana de prueba que ningun articulo del catalogo declara
     * seria el unico plazo del modelo sin origen auditable, y el dominio ademas lo
     * impide: un contrato {@code TRIALING} sin {@code trialEndDate} no se
     * construye.
     *
     * <p>
     * {@code graceDays} va en {@code null} a proposito: lo resuelve
     * {@code CreateSubscriptionService} desde {@code platform_billing_config}, que
     * es el unico sitio que decide el valor por defecto. Duplicarlo aqui es
     * exactamente como divergieron los dos caminos de alta (#467).
     */
    private CreateSubscriptionCommand newContract(Long companyId, SubscriptionQuoteSnapshot quote,
            String actor, List<SubscriptionItemLineCommand> lines, LocalDate today) {
        BillingCycle cycle = quote.billingCycle();
        int trialDays = platformCatalogPort.findInitialContractTemplate(cycle)
                .map(InitialContractTemplate::defaultTrialDays).orElse(0);
        boolean withTrial = trialDays > 0;
        // Periodo semiabierto, igual que el del contrato inicial: arranca hoy y el
        // ultimo dia cubierto es la vispera del siguiente ciclo.
        LocalDate periodEnd = today
                .plus(cycle == BillingCycle.ANNUAL ? Period.ofYears(1) : Period.ofMonths(1))
                .minusDays(1);
        return new CreateSubscriptionCommand(companyId, quote.id(), quote.priceListId(), cycle,
                withTrial ? SubscriptionStatus.TRIALING : SubscriptionStatus.ACTIVE, today,
                withTrial ? today.plusDays(trialDays) : null, today, periodEnd,
                periodEnd.plusDays(1), null, null, true, actor, lines);
    }
}
