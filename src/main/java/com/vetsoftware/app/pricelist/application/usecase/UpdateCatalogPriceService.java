package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.command.UpdateCatalogPriceCommand;
import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import com.vetsoftware.app.pricelist.application.port.in.UpdateCatalogPriceUseCase;
import com.vetsoftware.app.pricelist.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import com.vetsoftware.app.pricelist.domain.CatalogPriceNotFoundException;
import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambia un tramo de precio.
 *
 * <p>
 * Es el camino por el que R9 se rompe si nadie lo guarda: el precio es hijo de
 * la lista y la lista es la que esta publicada, asi que la comprobacion no
 * puede vivir en {@code CatalogPrice} -no sabe en que estado esta su lista- ni
 * en la base -un CHECK no lee otra tabla-. Se carga la lista, se bloquea y se
 * le pide {@code requireDraft()} antes de tocar nada.
 */
@Observed(name = "pricelist.catalogprice.update")
@Service
public class UpdateCatalogPriceService implements UpdateCatalogPriceUseCase {

    private final CatalogPriceRepository repository;
    private final PriceListRepository priceListRepository;

    /**
     * Solo para el resumen del articulo de la respuesta: aqui no hay FK que validar
     * -{@code catalogItemId} es {@code final} en el dominio y no se puede
     * reapuntar-, asi que este puerto es puramente de lectura. Sin el, {@code PUT}
     * devolveria un {@code catalogItem} vacio mientras el listado lo trae, y el
     * cliente tendria dos formas del mismo recurso (incidencia #379).
     */
    private final CatalogItemQueryPort catalogItemQueryPort;

    public UpdateCatalogPriceService(CatalogPriceRepository repository,
            PriceListRepository priceListRepository, CatalogItemQueryPort catalogItemQueryPort) {
        this.repository = repository;
        this.priceListRepository = priceListRepository;
        this.catalogItemQueryPort = catalogItemQueryPort;
    }

    @Override
    @Transactional
    public CatalogPriceDto execute(UpdateCatalogPriceCommand command) {
        CatalogPrice price = repository.findById(command.id())
                .orElseThrow(() -> new CatalogPriceNotFoundException(command.id()));
        PriceList priceList = priceListRepository.lockById(price.getPriceListId())
                .orElseThrow(() -> new PriceListNotFoundException(price.getPriceListId()));
        priceList.requireDraft();

        price.update(command.billingCycle(), command.tierMin(), command.tierMax(),
                command.includedQuantity(), command.unitAmount(), command.setupAmount(),
                command.taxRate(), command.taxTreatment());
        CatalogPrice.requireNoTierOverlap(price, repository.findTierScope(price.getPriceListId(),
                price.getCatalogItemId(), price.getBillingCycle()));
        return CatalogPriceDto.from(repository.save(price),
                catalogItemQueryPort.findById(price.getCatalogItemId()).orElse(null));
    }
}
