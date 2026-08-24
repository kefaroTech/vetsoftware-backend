package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.port.in.DeleteCatalogPriceUseCase;
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
 * Borrar un precio de una lista publicada es exactamente el escenario de R9: la
 * fila desaparece del {@code @SQLRestriction} y el importe que se le ofrecio a
 * un cliente deja de existir. Se rechaza igual que una edicion.
 */
@Observed(name = "pricelist.catalogprice.delete")
@Service
public class DeleteCatalogPriceService implements DeleteCatalogPriceUseCase {

    private final CatalogPriceRepository repository;
    private final PriceListRepository priceListRepository;

    public DeleteCatalogPriceService(CatalogPriceRepository repository,
            PriceListRepository priceListRepository) {
        this.repository = repository;
        this.priceListRepository = priceListRepository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        CatalogPrice price = repository.findById(id)
                .orElseThrow(() -> new CatalogPriceNotFoundException(id));
        PriceList priceList = priceListRepository.lockById(price.getPriceListId())
                .orElseThrow(() -> new PriceListNotFoundException(price.getPriceListId()));
        priceList.requireDraft();
        repository.delete(id);
    }
}
