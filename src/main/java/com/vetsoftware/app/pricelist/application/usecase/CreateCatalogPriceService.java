package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.command.CreateCatalogPriceCommand;
import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import com.vetsoftware.app.pricelist.application.dto.LinkStateDto;
import com.vetsoftware.app.pricelist.application.port.in.CreateCatalogPriceUseCase;
import com.vetsoftware.app.pricelist.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.pricelist.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import com.vetsoftware.app.pricelist.domain.CatalogPriceNotFoundException;
import com.vetsoftware.app.pricelist.domain.CatalogPriceTierOverlapException;
import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anade un tramo de precio a una lista.
 *
 * <p>
 * El orden de los pasos es la regla, no una preferencia: primero se bloquea la
 * lista, despues se comprueba que sigue en DRAFT (R9), luego se mira el solape
 * y solo al final se mira si el tramo exacto lo ocupa una fila retirada.
 * Bloquear antes de leer es lo que impide que dos altas concurrentes lean el
 * mismo conjunto de hermanos y pasen las dos.
 */
@Observed(name = "pricelist.catalogprice.create")
@Service
public class CreateCatalogPriceService implements CreateCatalogPriceUseCase {

    private final CatalogPriceRepository repository;
    private final PriceListRepository priceListRepository;
    private final CatalogItemValidationPort catalogItemValidationPort;
    private final CatalogItemQueryPort catalogItemQueryPort;
    private final Clock clock;

    public CreateCatalogPriceService(CatalogPriceRepository repository,
            PriceListRepository priceListRepository,
            CatalogItemValidationPort catalogItemValidationPort,
            CatalogItemQueryPort catalogItemQueryPort, Clock clock) {
        this.repository = repository;
        this.priceListRepository = priceListRepository;
        this.catalogItemValidationPort = catalogItemValidationPort;
        this.catalogItemQueryPort = catalogItemQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CatalogPriceDto execute(CreateCatalogPriceCommand command) {
        PriceList priceList = priceListRepository.lockById(command.priceListId())
                .orElseThrow(() -> new PriceListNotFoundException(command.priceListId()));
        priceList.requireDraft();
        if (!catalogItemValidationPort.existsById(command.catalogItemId()))
            throw new IllegalArgumentException(
                    "Catalog item not found: " + command.catalogItemId());

        CatalogPrice candidate = CatalogPrice.create(command.priceListId(), command.catalogItemId(),
                command.billingCycle(), command.tierMin(), command.tierMax(),
                command.includedQuantity(), command.unitAmount(), command.setupAmount(),
                command.taxRate(), command.taxTreatment(), LocalDateTime.now(clock));
        CatalogPrice.requireNoTierOverlap(candidate, repository.findTierScope(command.priceListId(),
                command.catalogItemId(), command.billingCycle()));

        Optional<LinkStateDto> existente = repository.findAnyByTier(command.priceListId(),
                command.catalogItemId(), command.billingCycle(), command.tierMin());
        if (existente.isPresent()) {
            return revivir(existente.get(), command);
        }
        return conResumen(repository.save(candidate));
    }

    /**
     * El resumen del articulo se resuelve tambien en el camino de escritura para
     * que los cuatro verbos de {@code /catalog-prices} devuelvan la misma forma: un
     * {@code catalogItem} presente al listar y ausente al crear obligaria al
     * cliente a escribir dos tratamientos para el mismo recurso, y haria falso lo
     * que dice el {@code @Schema} ("vacio solo si el articulo se retiro del
     * catalogo"). Incidencia #379.
     *
     * <p>
     * La guarda de existencia sigue siendo {@link CatalogItemValidationPort}: es
     * una comprobacion de invariante y no necesita traer columnas. Este puerto es
     * el de lectura, y se consulta una sola vez, al final.
     */
    private CatalogPriceDto conResumen(CatalogPrice price) {
        return CatalogPriceDto.from(price,
                catalogItemQueryPort.findById(price.getCatalogItemId()).orElse(null));
    }

    /**
     * La comprobacion de solape que va justo antes solo ve los tramos activos —y es
     * correcto que asi sea: un tramo retirado no compite por ninguna unidad—, pero
     * {@code uq_catalog_prices_tier} no ignora {@code enabled}, asi que esa fila
     * sigue ocupando la clave. Reactivarla y reescribirla es lo que convierte
     * «quitar un tramo y volver a ponerlo» en una operacion normal en vez de un 409
     * opaco sobre dinero.
     *
     * <p>
     * La rama {@code enabled} es teoricamente inalcanzable —un tramo activo con el
     * mismo {@code tier_min} solapa por definicion y ya lo habria rechazado
     * {@code requireNoTierOverlap}—, pero se escribe igual: depender de esa
     * deduccion es depender de que nadie reordene los dos pasos.
     */
    private CatalogPriceDto revivir(LinkStateDto estado, CreateCatalogPriceCommand command) {
        if (estado.enabled()) {
            throw new CatalogPriceTierOverlapException(command.priceListId(),
                    command.catalogItemId(), command.billingCycle(), command.tierMin(),
                    command.tierMax(), estado.id());
        }
        repository.reactivate(estado.id());
        CatalogPrice revivido = repository.findById(estado.id())
                .orElseThrow(() -> new CatalogPriceNotFoundException(estado.id()));
        revivido.update(command.billingCycle(), command.tierMin(), command.tierMax(),
                command.includedQuantity(), command.unitAmount(), command.setupAmount(),
                command.taxRate(), command.taxTreatment());
        return conResumen(repository.save(revivido));
    }
}
