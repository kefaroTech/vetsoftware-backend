package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.catalogitem.application.port.in.ListCatalogItemsUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "catalogitem.list")
@Service
public class ListCatalogItemsService implements ListCatalogItemsUseCase {

    private final CatalogItemRepository repository;

    public ListCatalogItemsService(CatalogItemRepository repository) {
        this.repository = repository;
    }

    /**
     * Los totales son los de la consulta y no se recalculan sobre el contenido ya
     * paginado: {@code PageResult.map} los conserva intactos, que es justo lo que
     * evita reportar «20 de 20» en un catálogo de cincuenta artículos.
     */
    @Override
    public PageResult<CatalogItemDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(CatalogItemDto::from);
    }
}
