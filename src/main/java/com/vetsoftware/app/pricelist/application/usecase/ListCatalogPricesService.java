package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import com.vetsoftware.app.pricelist.application.port.in.ListCatalogPricesUseCase;
import com.vetsoftware.app.pricelist.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.CatalogItemRef;
import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import java.util.Map;
import org.springframework.stereotype.Service;

@Observed(name = "pricelist.catalogprice.list")
@Service
public class ListCatalogPricesService implements ListCatalogPricesUseCase {

    private final CatalogPriceRepository repository;
    private final PriceListRepository priceListRepository;
    private final CatalogItemQueryPort catalogItemQueryPort;

    public ListCatalogPricesService(CatalogPriceRepository repository,
            PriceListRepository priceListRepository, CatalogItemQueryPort catalogItemQueryPort) {
        this.repository = repository;
        this.priceListRepository = priceListRepository;
        this.catalogItemQueryPort = catalogItemQueryPort;
    }

    /**
     * Comprueba que la lista existe antes de listar: una pagina vacia y una lista
     * inexistente son respuestas distintas, y confundirlas hace que una tarifa
     * borrada parezca una tarifa sin precios.
     */
    @Override
    public PageResult<CatalogPriceDto> listByPriceList(Long priceListId, int page, int pageSize) {
        if (priceListRepository.findById(priceListId).isEmpty())
            throw new PriceListNotFoundException(priceListId);
        PageResult<CatalogPrice> pagina = repository.findAllByPriceListId(priceListId, page,
                pageSize);
        // Una consulta para toda la pagina y no una por fila: el N+1 que la incidencia
        // #379 describia del lado del cliente no se arregla trayendolo al servidor.
        Map<Long, CatalogItemRef> articulos = catalogItemQueryPort.findAllByIds(
                pagina.content().stream().map(CatalogPrice::getCatalogItemId).toList());
        return pagina
                .map(price -> CatalogPriceDto.from(price, articulos.get(price.getCatalogItemId())));
    }
}
