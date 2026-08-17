package com.vetsoftware.app.inventory.application.port.out;

import com.vetsoftware.app.inventory.application.command.SearchKardexCommand;
import com.vetsoftware.app.inventory.application.command.SearchPurchasesQuery;
import com.vetsoftware.app.inventory.application.command.SearchStockCommand;
import com.vetsoftware.app.inventory.application.dto.InventoryAlertsView;
import com.vetsoftware.app.inventory.application.dto.InventoryValuationView;
import com.vetsoftware.app.inventory.application.dto.KardexExportRow;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.inventory.application.dto.PurchaseView;
import com.vetsoftware.app.inventory.application.dto.StockLotView;
import com.vetsoftware.app.inventory.application.dto.StockMovementView;
import com.vetsoftware.app.inventory.application.dto.StockView;
import java.time.LocalDate;
import java.util.List;

/**
 * Lecturas del inventario (read model): saldo por sede, lotes y kardex,
 * resolviendo nombres de producto/sede.
 */
public interface StockQueryPort {
    PageResult<StockView> searchStock(SearchStockCommand command);

    List<StockLotView> listLots(Long companyId, Long branchId, Long productId);

    PageResult<StockMovementView> searchKardex(SearchKardexCommand command);

    /**
     * Alertas: productos bajo mínimo + lotes que vencen dentro de
     * {@code expiringInDays} (o ya vencidos).
     */
    InventoryAlertsView alerts(Long companyId, Long branchId, int expiringInDays);

    /** Valuación: Σ (lote.disponible × lote.costo), total y por producto. */
    InventoryValuationView valuation(Long companyId, Long branchId);

    /** Libro de compras: movimientos de entrada (PURCHASE) paginados. */
    PageResult<PurchaseView> purchases(SearchPurchasesQuery query);

    // ── Exportación (sin paginar, orden ascendente por fecha)
    // ─────────────────────

    /**
     * Todos los movimientos del kardex (con nombres resueltos) en orden ascendente,
     * para el reporte con saldo corrido.
     */
    List<KardexExportRow> kardexForExport(SearchKardexCommand command);

    /**
     * Saldo del producto justo ANTES de {@code from} (suma con signo), para el
     * saldo inicial del rango.
     */
    int openingBalance(Long companyId, Long productId, Long branchId, LocalDate from);

    /**
     * Todas las compras (entradas) del rango en orden ascendente, para el reporte
     * del libro de compras.
     */
    List<PurchaseView> purchasesForExport(SearchPurchasesQuery query);
}
