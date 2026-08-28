package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.CreateInitialSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import com.vetsoftware.app.subscription.application.dto.InitialCapacityTemplate;
import com.vetsoftware.app.subscription.application.dto.InitialContractTemplate;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.CreateInitialSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.CreateSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.out.PlatformCatalogPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.PlatformCatalogNotConfiguredForSubscriptionException;
import com.vetsoftware.app.subscription.domain.StructuralCapacityMinimum;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El contrato con el que nace una empresa.
 *
 * <p>
 * Resuelve el minimo estructural de la plataforma, lo <strong>congela</strong>
 * en una linea de {@code origin = 'INITIAL'} y delega el alta en
 * {@link CreateSubscriptionUseCase}, que es quien sabe crear un contrato. No
 * duplica esa logica: aqui lo unico propio es <em>de donde salen los datos</em>
 * cuando nadie los negocio.
 *
 * <p>
 * <strong>Si falta cualquier pieza del minimo, lanza y no crea nada.</strong>
 * Va dentro de la transaccion del alta de la empresa, asi que revertir aqui
 * revierte el alta entera —que es la conducta correcta: una empresa sin
 * contrato entra al sistema, no tiene permisos calculados y no puede hacer
 * nada, sin ningun mensaje que lo explique—.
 */
@Observed(name = "subscription.create.initial")
@Service
public class CreateInitialSubscriptionService implements CreateInitialSubscriptionUseCase {

    /** El actor que queda en la bitacora: el contrato inicial no lo pide nadie. */
    private static final String ACTOR = "platform-signup";

    private final PlatformCatalogPort platformCatalogPort;
    private final CreateSubscriptionUseCase createSubscriptionUseCase;
    private final Clock clock;

    public CreateInitialSubscriptionService(PlatformCatalogPort platformCatalogPort,
            CreateSubscriptionUseCase createSubscriptionUseCase, Clock clock) {
        this.platformCatalogPort = platformCatalogPort;
        this.createSubscriptionUseCase = createSubscriptionUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionDto execute(CreateInitialSubscriptionCommand command) {
        if (command.companyId() == null)
            throw new IllegalArgumentException("companyId is required");
        BillingCycle cycle = command.billingCycle() == null
                ? BillingCycle.MONTHLY
                : command.billingCycle();
        LocalDate start = command.startDate() == null ? LocalDate.now(clock) : command.startDate();

        InitialContractTemplate template = platformCatalogPort.findInitialContractTemplate(cycle)
                .orElseThrow(() -> new PlatformCatalogNotConfiguredForSubscriptionException(
                        command.companyId()));
        List<InitialCapacityTemplate> capacities = platformCatalogPort
                .findInitialCapacityTemplates(cycle);
        requireOperableMinimum(command.companyId(), capacities);

        boolean withTrial = template.defaultTrialDays() > 0;
        // El periodo es semiabierto igual que la vigencia de las lineas: arranca hoy y
        // el ultimo dia cubierto es la vispera del siguiente ciclo.
        LocalDate periodEnd = start.plus(cycle == BillingCycle.ANNUAL
                ? java.time.Period.ofYears(1)
                : java.time.Period.ofMonths(1)).minusDays(1);

        return createSubscriptionUseCase.execute(
                new CreateSubscriptionCommand(command.companyId(), null, template.priceListId(),
                        cycle, withTrial ? SubscriptionStatus.TRIALING : SubscriptionStatus.ACTIVE,
                        start, withTrial ? start.plusDays(template.defaultTrialDays()) : null,
                        // Los dias de gracia van en null a proposito: los resuelve
                        // CreateSubscriptionService desde platform_billing_config, que
                        // es el unico sitio que decide el valor por defecto. Pasar aqui
                        // template.defaultGraceDays() volveria a poner el mismo numero
                        // en dos servicios, que es como los dos caminos divergieron
                        // (#467).
                        start, periodEnd, periodEnd.plusDays(1), null, null, true, ACTOR,
                        initialLines(template, capacities, start)));
    }

    /**
     * El contrato inicial es un <strong>conjunto</strong> de lineas, no una: la del
     * nucleo, que abre las pantallas, y una por cada capacidad del minimo, que abre
     * las cantidades. Firmar solo la primera era firmar un contrato que no permitia
     * crear ni la sede principal del propio alta (#490).
     */
    private static List<SubscriptionItemLineCommand> initialLines(InitialContractTemplate template,
            List<InitialCapacityTemplate> capacities, LocalDate start) {
        List<SubscriptionItemLineCommand> lines = new ArrayList<>();
        lines.add(coreLine(template, start));
        for (InitialCapacityTemplate capacity : capacities) {
            lines.add(capacityLine(capacity, start));
        }
        return List.copyOf(lines);
    }

    /**
     * Un contrato inicial que no concede las capacidades minimas no se firma.
     *
     * <p>
     * <strong>Se falla aqui y no mas adelante a proposito.</strong> La alternativa
     * —firmar lo que haya— produce una empresa viva cuyo alta muere tres pasos
     * despues con un {@code 404} sobre una unidad de capacidad, apuntando al
     * recalculo de permisos en vez de al catalogo, que es donde de verdad falta
     * algo. Es el sintoma exacto que costo tres capas de diagnostico.
     *
     * <p>
     * Y se falla <em>cerrado</em>: no se inventa un techo por defecto. Un techo que
     * no sale de una linea de contrato no lo puede explicar despues nadie que
     * audite la factura.
     */
    private static void requireOperableMinimum(Long companyId,
            List<InitialCapacityTemplate> capacities) {
        // LinkedHashSet y no un conjunto de enumerado: desde el 333 la unidad es el
        // codigo del eje y ya no hay tipo cerrado que enumerar. Se filtran los nulos
        // porque un conjunto con null dentro haria que removeAll se comportara segun
        // la implementacion en vez de segun la regla.
        Set<String> granted = capacities.stream().map(InitialCapacityTemplate::capacityUnit)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> missing = StructuralCapacityMinimum.missingFrom(granted);
        if (!missing.isEmpty()) {
            throw new PlatformCatalogNotConfiguredForSubscriptionException(companyId, missing);
        }
    }

    /**
     * Una linea de capacidad, con sus valores congelados igual que la del nucleo.
     * El tipo va literal —y no copiado de la fila— porque la consulta que la trae
     * ya filtra {@code item_type = 'CAPACITY'}: leerlo de otro sitio abriria la
     * puerta a firmar una unidad colgada de un modulo, que el dominio y
     * {@code chk_subscription_items_capacity_unit} rechazan en los dos sentidos.
     *
     * <p>
     * La cantidad es {@code min_quantity} del articulo, con suelo 1: el techo que
     * acaba en {@code company_capacities} es {@code included_quantity + quantity}
     * ({@code CapacityGrantLine.ceiling()}), asi que una capacidad firmada con
     * cantidad cero volveria a dejar la empresa sin poder crear nada.
     */
    private static SubscriptionItemLineCommand capacityLine(InitialCapacityTemplate capacity,
            LocalDate start) {
        // Tramo unico y abierto y sin descuento: el alta inicial firma la cantidad
        // minima del articulo, que siempre cae en el primer tramo, y no negocia nada.
        return new SubscriptionItemLineCommand(capacity.catalogItemId(), capacity.itemCode(),
                capacity.itemName(), SubscriptionItemType.CAPACITY, capacity.capacityUnit(), 1,
                null, capacity.includedQuantity(), capacity.taxTreatment(),
                Math.max(capacity.minQuantity(), 1), capacity.unitAmount(), null, null, false,
                capacity.taxRate(), start, null);
    }

    /**
     * La linea del nucleo, con sus valores congelados. La cantidad es
     * {@code min_quantity} del articulo —un nucleo es 1— y no un literal, para que
     * un catalogo que exija mas de uno no quede mal firmado desde el primer dia.
     */
    private static SubscriptionItemLineCommand coreLine(InitialContractTemplate template,
            LocalDate start) {
        return new SubscriptionItemLineCommand(template.catalogItemId(), template.itemCode(),
                template.itemName(), template.itemType(), template.capacityUnit(), 1, null,
                template.includedQuantity(), template.taxTreatment(),
                Math.max(template.minQuantity(), 1), template.unitAmount(), null, null, false,
                template.taxRate(), start, null);
    }

}
