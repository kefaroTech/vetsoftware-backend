package com.vetsoftware.app.catalogitem.application.port.out;

import com.vetsoftware.app.catalogitem.application.dto.LinkStateDto;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModule;
import java.util.List;
import java.util.Optional;

public interface CatalogItemSubModuleRepository {

    CatalogItemSubModule save(CatalogItemSubModule link);

    Optional<CatalogItemSubModule> findById(Long id);

    List<CatalogItemSubModule> findAllByCatalogItemId(Long catalogItemId);

    void delete(Long id);

    int reactivate(Long id);

    /** El par, ignorando el borrado lógico. Ver {@link LinkStateDto}. */
    Optional<LinkStateDto> findAnyByPair(Long catalogItemId, Long subModuleId);

    boolean existsActiveByCatalogItemId(Long catalogItemId);
}
