package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemSubModuleDto;
import com.vetsoftware.app.catalogitem.application.port.in.ListCatalogItemSubModulesUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemSubModuleRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "catalogitem.submodule.list")
@Service
public class ListCatalogItemSubModulesService implements ListCatalogItemSubModulesUseCase {

    private final CatalogItemSubModuleRepository repository;
    private final CatalogItemRepository catalogItemRepository;

    public ListCatalogItemSubModulesService(CatalogItemSubModuleRepository repository,
            CatalogItemRepository catalogItemRepository) {
        this.repository = repository;
        this.catalogItemRepository = catalogItemRepository;
    }

    /**
     * Comprueba que el artículo existe antes de listar: devolver una lista vacía
     * para un id inexistente le dice al cliente «este artículo no abre nada» cuando
     * la verdad es «este artículo no existe».
     */
    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemSubModuleDto> listByCatalogItem(Long catalogItemId) {
        catalogItemRepository.findById(catalogItemId)
                .orElseThrow(() -> new CatalogItemNotFoundException(catalogItemId));
        return repository.findAllByCatalogItemId(catalogItemId).stream()
                .map(CatalogItemSubModuleDto::from).toList();
    }
}
