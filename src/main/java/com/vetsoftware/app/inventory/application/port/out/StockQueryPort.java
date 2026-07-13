package com.vetsoftware.app.inventory.application.port.out;

import com.vetsoftware.app.inventory.application.command.SearchKardexCommand;
import com.vetsoftware.app.inventory.application.command.SearchPurchasesQuery;
import com.vetsoftware.app.inventory.application.command.SearchStockCommand;
import com.vetsoftware.app.inventory.application.dto.InventoryAlertsView;
import com.vetsoftware.app.inventory.application.dto.InventoryValuationView;
import com.vetsoftware.app.inventory.application.dto.PageResult;
import com.vetsoftware.app.inventory.application.dto.PurchaseView;
import com.vetsoftware.app.inventory.application.dto.StockLotView;
import com.vetsoftware.app.inventory.application.dto.StockMovementView;
import com.vetsoftware.app.inventory.application.dto.StockView;
import java.util.List;

/** Lecturas del inventario (read model): saldo por sede, lotes y kardex, resolviendo nombres de producto/sede. */
public interface StockQueryPort {
    PageResult<StockView> searchStock(SearchStockCommand command);
    List<StockLotView> listLots(Long companyId, Long branchId, Long productId);
    PageResult<StockMovementView> searchKardex(SearchKardexCommand command);

    /** Alertas: productos bajo mínimo + lotes que vencen dentro de {@code expiringInDays} (o ya vencidos). */
    InventoryAlertsView alerts(Long companyId, Long branchId, int expiringInDays);

    /** Valuación: Σ (lote.disponible × lote.costo), total y por producto. */
    InventoryValuationView valuation(Long companyId, Long branchId);

    /** Libro de compras: movimientos de entrada (PURCHASE) paginados. */
    PageResult<PurchaseView> purchases(SearchPurchasesQuery query);
}
