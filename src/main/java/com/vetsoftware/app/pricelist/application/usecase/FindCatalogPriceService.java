package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import com.vetsoftware.app.pricelist.application.port.in.FindCatalogPriceUseCase;
import com.vetsoftware.app.pricelist.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.domain.CatalogPriceNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "pricelist.catalogprice.find")
@Service
public class FindCatalogPriceService implements FindCatalogPriceUseCase {

    private final CatalogPriceRepository repository;
    private final CatalogItemQueryPort catalogItemQueryPort;

    public FindCatalogPriceService(CatalogPriceRepository repository,
            CatalogItemQueryPort catalogItemQueryPort) {
        this.repository = repository;
        this.catalogItemQueryPort = catalogItemQueryPort;
    }

    /**
     * Trae tambien el resumen del articulo para que la ficha y el listado hablen el
     * mismo contrato: que el nombre aparezca solo en uno de los dos obliga al
     * cliente a escribir dos tratamientos para la misma fila (incidencia #379).
     */
    @Override
    public CatalogPriceDto findById(Long id) {
        return repository.findById(id)
                .map(price -> CatalogPriceDto.from(price,
                        catalogItemQueryPort.findById(price.getCatalogItemId()).orElse(null)))
                .orElseThrow(() -> new CatalogPriceNotFoundException(id));
    }
}
