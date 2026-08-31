package com.vetsoftware.app.catalogitemaihint.application.usecase;

import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.catalogitemaihint.application.port.in.FindCurrentCatalogItemAiHintUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHintNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La pista que rige hoy para un articulo. Un articulo sin pista es un 404 y no
 * una respuesta vacia: «no tiene» y «no la encuentro» son el mismo estado aqui,
 * y el front necesita distinguirlo de «tiene una y viene sin texto».
 */
@Service
public class FindCurrentCatalogItemAiHintService implements FindCurrentCatalogItemAiHintUseCase {

    private final CatalogItemAiHintRepository repository;
    private final CatalogItemQueryPort catalogItemQueryPort;

    public FindCurrentCatalogItemAiHintService(CatalogItemAiHintRepository repository,
            CatalogItemQueryPort catalogItemQueryPort) {
        this.repository = repository;
        this.catalogItemQueryPort = catalogItemQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogItemAiHintDto findCurrentByCatalogItemId(Long catalogItemId) {
        return repository.findCurrentByCatalogItemId(catalogItemId)
                .map(hint -> CatalogItemAiHintDto.from(hint,
                        catalogItemQueryPort.findById(catalogItemId).orElse(null)))
                .orElseThrow(() -> new CatalogItemAiHintNotFoundException(catalogItemId));
    }
}
