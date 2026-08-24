package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.port.in.DeleteCatalogItemDependencyUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemDependencyRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependency;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "catalogitem.dependency.delete")
@Service
public class DeleteCatalogItemDependencyService implements DeleteCatalogItemDependencyUseCase {

    private final CatalogItemDependencyRepository repository;

    public DeleteCatalogItemDependencyService(CatalogItemDependencyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long catalogItemId, Long id) {
        CatalogItemDependency dependency = repository.findById(id)
                .orElseThrow(() -> new CatalogItemDependencyNotFoundException(id));
        if (!dependency.getCatalogItemId().equals(catalogItemId)) {
            throw new CatalogItemDependencyNotFoundException(id);
        }
        repository.delete(id);
    }
}
