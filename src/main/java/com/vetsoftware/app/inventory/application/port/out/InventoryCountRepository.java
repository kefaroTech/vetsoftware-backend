package com.vetsoftware.app.inventory.application.port.out;

import com.vetsoftware.app.inventory.application.command.SearchCountsQuery;
import com.vetsoftware.app.inventory.application.dto.InventoryCountView;
import com.vetsoftware.app.inventory.application.dto.PageResult;
import com.vetsoftware.app.inventory.domain.InventoryCount;
import java.util.Optional;

public interface InventoryCountRepository {
    /** Persiste la sesión con sus líneas y devuelve el agregado con id y fecha asignados. */
    InventoryCount save(InventoryCount count);

    /** Listado paginado de sesiones (resumen, sin líneas) por empresa y sede opcional, más reciente primero. */
    PageResult<InventoryCountView> search(SearchCountsQuery query);

    /** Detalle de una sesión (con líneas) validando que sea de la empresa. */
    Optional<InventoryCountView> findDetail(Long companyId, Long id);
}
