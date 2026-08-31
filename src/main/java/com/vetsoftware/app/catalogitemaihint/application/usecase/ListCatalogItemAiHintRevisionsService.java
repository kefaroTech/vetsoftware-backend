package com.vetsoftware.app.catalogitemaihint.application.usecase;

import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.catalogitemaihint.application.port.in.ListCatalogItemAiHintRevisionsUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemRef;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El historial de un articulo.
 *
 * <p>
 * El articulo se resuelve <b>una vez</b> para la pagina entera: todas las
 * revisiones son del mismo, asi que un {@code findAllByIds} aqui pediria n
 * veces la misma fila.
 */
@Service
public class ListCatalogItemAiHintRevisionsService
        implements
            ListCatalogItemAiHintRevisionsUseCase {

    private final CatalogItemAiHintRepository repository;
    private final CatalogItemQueryPort catalogItemQueryPort;

    public ListCatalogItemAiHintRevisionsService(CatalogItemAiHintRepository repository,
            CatalogItemQueryPort catalogItemQueryPort) {
        this.repository = repository;
        this.catalogItemQueryPort = catalogItemQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CatalogItemAiHintDto> listByCatalogItemId(Long catalogItemId, int page,
            int pageSize) {
        CatalogItemRef articulo = catalogItemQueryPort.findById(catalogItemId).orElse(null);
        return repository.findAllByCatalogItemId(catalogItemId, page, pageSize)
                .map(hint -> CatalogItemAiHintDto.from(hint, articulo));
    }
}
