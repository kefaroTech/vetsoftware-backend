package com.vetsoftware.app.inventory.application.usecase;

import com.vetsoftware.app.inventory.application.command.InventoryAlertsQuery;
import com.vetsoftware.app.inventory.application.command.InventoryValuationQuery;
import com.vetsoftware.app.inventory.application.command.ListLotsCommand;
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
import com.vetsoftware.app.inventory.application.port.in.GetInventoryAlertsUseCase;
import com.vetsoftware.app.inventory.application.port.in.GetInventoryValuationUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListKardexUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListProductLotsUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListPurchasesUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListStockUseCase;
import com.vetsoftware.app.inventory.application.port.out.StockQueryPort;
import java.util.List;
import org.springframework.stereotype.Service;

/** Lecturas del inventario. El gate {@code inventory.read} vive en los @PreAuthorize de los puertos de entrada. */
@Service
public class InventoryQueryService implements ListStockUseCase, ListProductLotsUseCase, ListKardexUseCase,
        GetInventoryAlertsUseCase, GetInventoryValuationUseCase, ListPurchasesUseCase {

    private final StockQueryPort stockQueryPort;

    public InventoryQueryService(StockQueryPort stockQueryPort) {
        this.stockQueryPort = stockQueryPort;
    }

    @Override
    public InventoryAlertsView alerts(InventoryAlertsQuery query) {
        return stockQueryPort.alerts(query.companyId(), query.branchId(), query.expiringInDays());
    }

    @Override
    public InventoryValuationView valuation(InventoryValuationQuery query) {
        return stockQueryPort.valuation(query.companyId(), query.branchId());
    }

    @Override
    public PageResult<PurchaseView> purchases(SearchPurchasesQuery query) {
        return stockQueryPort.purchases(query);
    }

    @Override
    public PageResult<StockView> search(SearchStockCommand command) {
        return stockQueryPort.searchStock(command);
    }

    @Override
    public List<StockLotView> listLots(ListLotsCommand command) {
        return stockQueryPort.listLots(command.companyId(), command.branchId(), command.productId());
    }

    @Override
    public PageResult<StockMovementView> searchKardex(SearchKardexCommand command) {
        return stockQueryPort.searchKardex(command);
    }
}
