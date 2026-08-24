package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.port.in.DeletePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListHasActivePricesException;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dar de baja una lista es una mutacion, asi que hereda R9: solo se puede con
 * una lista en DRAFT. Una publicada se retira archivandola, que conserva la
 * fila visible y con ella la trazabilidad de lo que se le ofrecio a quien ya
 * firmo.
 */
@Observed(name = "pricelist.delete")
@Service
public class DeletePriceListService implements DeletePriceListUseCase {

    private final PriceListRepository repository;
    private final CatalogPriceRepository catalogPriceRepository;

    public DeletePriceListService(PriceListRepository repository,
            CatalogPriceRepository catalogPriceRepository) {
        this.repository = repository;
        this.catalogPriceRepository = catalogPriceRepository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        PriceList priceList = repository.findById(id)
                .orElseThrow(() -> new PriceListNotFoundException(id));
        priceList.requireDraft();
        long activePrices = catalogPriceRepository.countByPriceListId(id);
        if (activePrices > 0)
            throw new PriceListHasActivePricesException(id, activePrices);
        repository.delete(id);
    }
}
