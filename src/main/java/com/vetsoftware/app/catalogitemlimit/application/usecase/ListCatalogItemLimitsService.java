package com.vetsoftware.app.catalogitemlimit.application.usecase;

import com.vetsoftware.app.catalogitemlimit.application.dto.CatalogItemLimitDto;
import com.vetsoftware.app.catalogitemlimit.application.port.in.ListCatalogItemLimitsUseCase;
import com.vetsoftware.app.catalogitemlimit.application.port.out.CatalogItemLimitRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Los techos de fábrica de un artículo. */
@Service
public class ListCatalogItemLimitsService implements ListCatalogItemLimitsUseCase {

    private final CatalogItemLimitRepository repository;

    public ListCatalogItemLimitsService(CatalogItemLimitRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemLimitDto> listByCatalogItemId(Long catalogItemId) {
        return repository.findAllByCatalogItemId(catalogItemId).stream()
                .map(CatalogItemLimitDto::from).toList();
    }
}
