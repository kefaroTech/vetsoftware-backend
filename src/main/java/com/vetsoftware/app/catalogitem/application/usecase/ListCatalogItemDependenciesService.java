package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDependencyDto;
import com.vetsoftware.app.catalogitem.application.port.in.ListCatalogItemDependenciesUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemDependencyRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "catalogitem.dependency.list")
@Service
public class ListCatalogItemDependenciesService implements ListCatalogItemDependenciesUseCase {

    private final CatalogItemDependencyRepository repository;
    private final CatalogItemRepository catalogItemRepository;

    public ListCatalogItemDependenciesService(CatalogItemDependencyRepository repository,
            CatalogItemRepository catalogItemRepository) {
        this.repository = repository;
        this.catalogItemRepository = catalogItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemDependencyDto> listByCatalogItem(Long catalogItemId) {
        catalogItemRepository.findById(catalogItemId)
                .orElseThrow(() -> new CatalogItemNotFoundException(catalogItemId));
        return repository.findAllByCatalogItemId(catalogItemId).stream()
                .map(CatalogItemDependencyDto::from).toList();
    }
}
