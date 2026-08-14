package com.vetsoftware.app.inventory.application.port.out;

import com.vetsoftware.app.inventory.domain.StockMovement;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import java.util.List;

public interface StockMovementRepository {
    StockMovement save(StockMovement movement);

    /**
     * ¿Ya hay movimientos para esta referencia Y este producto? (idempotencia de
     * recordSale).
     *
     * <p>
     * El producto es parte de la clave, no un detalle. Una venta POS de N productos
     * emite N movimientos bajo el mismo documento: si la clave fuera solo la
     * referencia, la segunda línea y siguientes se descartarían como duplicado y el
     * stock quedaría sobrestimado sin que nada fallara.
     */
    boolean existsByReference(StockReferenceType referenceType, Long referenceId, Long productId);

    /** Movimientos de una referencia (para compensar/anular). */
    List<StockMovement> findByReferenceAndCompanyId(StockReferenceType referenceType,
            Long referenceId, Long companyId);
}
