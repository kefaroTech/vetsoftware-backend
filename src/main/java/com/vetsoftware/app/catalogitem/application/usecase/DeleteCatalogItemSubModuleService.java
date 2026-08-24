package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.port.in.DeleteCatalogItemSubModuleUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemSubModuleRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModule;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "catalogitem.submodule.delete")
@Service
public class DeleteCatalogItemSubModuleService implements DeleteCatalogItemSubModuleUseCase {

    private final CatalogItemSubModuleRepository repository;

    public DeleteCatalogItemSubModuleService(CatalogItemSubModuleRepository repository) {
        this.repository = repository;
    }

    /**
     * Un vínculo que no cuelga del artículo de la ruta se responde como
     * inexistente, no como prohibido: para quien pregunta por
     * {@code /catalog-items/7/sub-modules/99}, el 99 de otro artículo no existe.
     */
    @Override
    @Transactional
    public void execute(Long catalogItemId, Long id) {
        CatalogItemSubModule link = repository.findById(id)
                .orElseThrow(() -> new CatalogItemSubModuleNotFoundException(id));
        if (!link.getCatalogItemId().equals(catalogItemId)) {
            throw new CatalogItemSubModuleNotFoundException(id);
        }
        repository.delete(id);
    }
}
