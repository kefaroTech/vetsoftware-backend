package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.command.ResolveConfiguratorSelectionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorSelectionDto;
import com.vetsoftware.app.configurator.application.port.in.ResolveConfiguratorSelectionUseCase;
import com.vetsoftware.app.configurator.application.port.out.CapacityCeilingQueryPort;
import com.vetsoftware.app.configurator.application.port.out.CatalogItemDependencyQueryPort;
import com.vetsoftware.app.configurator.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.BillingCycle;
import com.vetsoftware.app.configurator.domain.CatalogItemRef;
import com.vetsoftware.app.configurator.domain.ConfiguratorAnswerCoherence;
import com.vetsoftware.app.configurator.domain.ConfiguratorAnswers;
import com.vetsoftware.app.configurator.domain.ConfiguratorResolver;
import com.vetsoftware.app.configurator.domain.IncludedCapacityDeduction;
import com.vetsoftware.app.configurator.domain.PublishedPriceListRef;
import com.vetsoftware.app.configurator.domain.RequiredItemsClosure;
import com.vetsoftware.app.configurator.domain.SelectedItem;
import com.vetsoftware.app.shared.pricing.PriceListValidity;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprueba, resuelve, <strong>resta lo ya incluido</strong> y traduce a
 * rotulos.
 *
 * <p>
 * <strong>El orden importa y es el punto del caso de uso.</strong> Resolver
 * primero y validar despues no serviria de nada: para cuando la seleccion esta
 * hecha, el articulo colado ya esta dentro. La comprobacion de coherencia es
 * una precondicion, no una revision.
 *
 * <p>
 * <strong>Por que este servicio ha pasado a saber de tarifas.</strong> Resolver
 * una seleccion <em>es</em> una operacion tarifada: «quince personas» no vale
 * nada sin saber cuantas trae ya puestas el contrato, y ese numero vive en
 * {@code catalog_prices}. La resta <b>no</b> podia ir en
 * {@link ConfiguratorResolver} —dominio puro; atarlo a una tarifa rompe la
 * separacion que ArchUnit vigila— asi que vive aqui, con un puerto explicito
 * ({@link CapacityCeilingQueryPort}) en vez de colada en el dominio. La regla
 * en si sigue siendo pura y comprobable sin base de datos:
 * {@link IncludedCapacityDeduction}.
 *
 * <p>
 * <strong>Que tarifa manda.</strong> La publicada vigente hoy, y lo decide
 * {@link PriceListValidity} sobre la fecha derivada del {@link Clock} inyectado
 * —la zona del negocio (D-81)— y no el motor de base de datos. Si hay varias
 * solapadas gana la de {@code validFrom} mas reciente y, a igualdad, la de id
 * mayor: determinista, en vez de «la primera que devuelva la consulta». Es el
 * mismo criterio que {@code GetPublicPlansService} y
 * {@code SelfServeQuoteService}, y tiene que serlo: si el configurador restara
 * el techo de una tarifa y la cotizacion cobrara con otra, las dos cifras
 * dejarian de cuadrar sin que nada fallara.
 *
 * <p>
 * <strong>Sin tarifa vigente no se resta nada</strong> y el carrito sale con
 * las cantidades en crudo. Es el lado seguro: restar un techo inventado
 * regalaria unidades, y devolver un carrito vacio dejaria la portada sin
 * propuesta por un dato de configuracion.
 */
@Observed(name = "configurator.selection.resolve")
@Service
public class ResolveConfiguratorSelectionService implements ResolveConfiguratorSelectionUseCase {

    private final ConfiguratorEffectRepository repository;
    private final ConfiguratorQuestionRepository questionRepository;
    private final ConfiguratorOptionRepository optionRepository;
    private final CatalogItemQueryPort catalogItemQueryPort;
    private final CapacityCeilingQueryPort capacityCeilingQueryPort;
    private final CatalogItemDependencyQueryPort dependencyQueryPort;
    private final Clock clock;

    public ResolveConfiguratorSelectionService(ConfiguratorEffectRepository repository,
            ConfiguratorQuestionRepository questionRepository,
            ConfiguratorOptionRepository optionRepository,
            CatalogItemQueryPort catalogItemQueryPort,
            CapacityCeilingQueryPort capacityCeilingQueryPort,
            CatalogItemDependencyQueryPort dependencyQueryPort, Clock clock) {
        this.repository = repository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.catalogItemQueryPort = catalogItemQueryPort;
        this.capacityCeilingQueryPort = capacityCeilingQueryPort;
        this.dependencyQueryPort = dependencyQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguratorSelectionDto resolve(ResolveConfiguratorSelectionCommand command) {
        ConfiguratorAnswers answers = new ConfiguratorAnswers(command.selectedOptionIds(),
                command.numericAnswers());
        ConfiguratorAnswerCoherence.assertCoherent(questionRepository.findAllOrdered(),
                optionRepository.findAllOrdered(), answers);

        List<SelectedItem> crudo = ConfiguratorResolver.resolve(repository.findAllOrdered(),
                answers);
        if (crudo.isEmpty()) {
            return new ConfiguratorSelectionDto(List.of());
        }
        List<SelectedItem> completo = RequiredItemsClosure.expand(crudo,
                dependencyQueryPort.findRequiredByItemId());
        List<CatalogItemRef> refs = catalogItemQueryPort
                .findActiveByIds(completo.stream().map(SelectedItem::catalogItemId).toList());

        List<SelectedItem> neto = IncludedCapacityDeduction.apply(completo, refs,
                techosVigentes(parseBillingCycle(command.billingCycle())));
        return ConfiguratorSelectionDto.from(neto, refs);
    }

    /**
     * El ciclo llega como texto porque el borde REST ya lo acota con un
     * {@code @Pattern}; esta comprobacion cubre la llamada directa al puerto, que
     * el camino de administracion tambien puede hacer. Un valor imposible muere
     * aqui con un mensaje de campo y no en un {@code valueOf} que saldria como un
     * 500.
     */
    private static BillingCycle parseBillingCycle(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("billingCycle is required");
        }
        try {
            return BillingCycle.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown billingCycle: " + raw);
        }
    }

    /**
     * El techo por eje de la tarifa vigente hoy, o vacio si hoy no rige ninguna.
     */
    private Map<String, Integer> techosVigentes(BillingCycle ciclo) {
        LocalDate hoy = LocalDateTime.now(clock).toLocalDate();
        Optional<PublishedPriceListRef> vigente = capacityCeilingQueryPort.findPublishedPriceLists()
                .stream()
                .filter(lista -> new PriceListValidity(lista.validFrom(), lista.validTo())
                        .isEffectiveOn(hoy))
                .max(Comparator.comparing(PublishedPriceListRef::validFrom)
                        .thenComparing(PublishedPriceListRef::id));
        return vigente.map(
                lista -> capacityCeilingQueryPort.findStructuralCeilingsByAxis(lista.id(), ciclo))
                .orElseGet(Map::of);
    }
}
