package com.vetsoftware.app.catalogitem.application.port.out;

import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

public interface CatalogItemRepository {

    CatalogItem save(CatalogItem item);

    Optional<CatalogItem> findById(Long id);

    PageResult<CatalogItem> findAll(int page, int pageSize);

    void delete(Long id);

    int reactivate(Long id);

    /**
     * Ignora el borrado lógico a propósito: {@code uq_catalog_items_code} tampoco
     * lo ignora, así que un artículo desactivado sigue ocupando su código y
     * reutilizarlo daría un error de integridad sin explicación.
     */
    boolean existsByCodeIgnoringEnabled(String code);
}
