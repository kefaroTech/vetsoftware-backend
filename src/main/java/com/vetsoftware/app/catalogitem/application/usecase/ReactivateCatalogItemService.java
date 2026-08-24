package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.catalogitem.application.port.in.ReactivateCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "catalogitem.reactivate")
@Service
public class ReactivateCatalogItemService implements ReactivateCatalogItemUseCase {

    private final CatalogItemRepository repository;

    public ReactivateCatalogItemService(CatalogItemRepository repository) {
        this.repository = repository;
    }

    /**
     * Decide si la fila existe por el número de filas actualizadas y no por una
     * lectura previa: el {@code @SQLRestriction} de la entidad esconde justo la
     * fila que se quiere reactivar, así que un {@code findById} devolvería vacío
     * para algo que sí está.
     */
    @Override
    @Transactional
    public CatalogItemDto execute(Long id) {
        if (repository.reactivate(id) == 0) {
            throw new CatalogItemNotFoundException(id);
        }
        return CatalogItemDto.from(
                repository.findById(id).orElseThrow(() -> new CatalogItemNotFoundException(id)));
    }
}
