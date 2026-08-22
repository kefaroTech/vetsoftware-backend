package com.vetsoftware.app.inventory.infrastructure.persistence;

import com.vetsoftware.app.inventory.application.command.SearchKardexCommand;
import com.vetsoftware.app.inventory.application.command.SearchPurchasesQuery;
import com.vetsoftware.app.inventory.application.command.SearchStockCommand;
import com.vetsoftware.app.inventory.application.dto.ExpiringLotView;
import com.vetsoftware.app.inventory.application.dto.InventoryAlertsView;
import com.vetsoftware.app.inventory.application.dto.InventoryValuationView;
import com.vetsoftware.app.inventory.application.dto.KardexExportRow;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.inventory.application.dto.ProductValuationView;
import com.vetsoftware.app.inventory.application.dto.PurchaseView;
import com.vetsoftware.app.inventory.application.dto.StockLotView;
import com.vetsoftware.app.inventory.application.dto.StockMovementView;
import com.vetsoftware.app.inventory.application.dto.StockView;
import com.vetsoftware.app.inventory.application.port.out.StockQueryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

@Repository
public class StockQueryAdapter implements StockQueryPort {

    /**
     * Marcador para el {@code IN} cuando no hay filtro por producto: el SQL nativo
     * no admite una lista vacía, así que la bandera apaga la condición y la lista
     * lleva un id imposible.
     */
    private static final List<Long> NO_PRODUCT_FILTER = List.of(-1L);

    private final StockBalanceJpaRepository balanceRepository;
    private final StockLotJpaRepository lotRepository;
    private final StockMovementJpaRepository movementRepository;

    public StockQueryAdapter(StockBalanceJpaRepository balanceRepository,
            StockLotJpaRepository lotRepository, StockMovementJpaRepository movementRepository) {
        this.balanceRepository = balanceRepository;
        this.lotRepository = lotRepository;
        this.movementRepository = movementRepository;
    }

    @Override
    public PageResult<StockView> searchStock(SearchStockCommand c) {
        String q = (c.q() == null || c.q().isBlank()) ? null : c.q().trim();
        // El alcance de empresa vive en el WHERE de la consulta y es lo que filtra
        // primero; los ids solo estrechan lo que esa empresa ya podia ver.
        boolean filterByProducts = !c.productIds().isEmpty();
        Page<StockRow> page = balanceRepository.searchStock(c.companyId(), c.branchId(), q,
                c.lowStock(), filterByProducts,
                filterByProducts ? c.productIds() : NO_PRODUCT_FILTER,
                Pages.request(c.page(), c.pageSize()));
        return Pages.result(page, StockQueryAdapter::toStockView);
    }

    @Override
    public List<StockLotView> listLots(Long companyId, Long branchId, Long productId) {
        return lotRepository.findLotsForView(companyId, productId, branchId).stream()
                .map(l -> new StockLotView(l.getId(), l.getLotNumber(), l.getExpireDate(),
                        l.getQuantityAvailable(), l.getUnitCost()))
                .toList();
    }

    @Override
    public PageResult<StockMovementView> searchKardex(SearchKardexCommand c) {
        LocalDateTime from = c.from() == null ? null : c.from().atStartOfDay();
        // `to` inclusivo por día → convertir a exclusivo al inicio del día siguiente.
        LocalDateTime to = c.to() == null ? null : c.to().plusDays(1).atStartOfDay();
        Page<StockMovementJpaEntity> page = movementRepository.searchKardex(c.companyId(),
                c.productId(), c.branchId(), from, to, Pages.request(c.page(), c.pageSize()));
        return Pages.result(page,
                m -> new StockMovementView(m.getId(), m.getLotId(), m.getType(), m.getQuantity(),
                        m.getUnitCost(), m.getReferenceType(), m.getReferenceId(), m.getReason(),
                        m.getCreatedBy(), m.getCreatedDate()));
    }

    @Override
    public InventoryAlertsView alerts(Long companyId, Long branchId, int expiringInDays) {
        List<StockView> lowStock = balanceRepository.lowStockRows(companyId, branchId).stream()
                .map(StockQueryAdapter::toStockView).toList();
        LocalDate limit = LocalDate.now().plusDays(Math.max(0, expiringInDays));
        List<ExpiringLotView> expiring = lotRepository.expiringLots(companyId, branchId, limit)
                .stream()
                .map(r -> new ExpiringLotView(r.getProductId(), r.getProductName(),
                        r.getProductCode(), r.getBranchId(), r.getBranchName(), r.getLotId(),
                        r.getLotNumber(), r.getExpireDate(), r.getQuantityAvailable(),
                        r.getDaysToExpire()))
                .toList();
        return new InventoryAlertsView(lowStock, expiring);
    }

    @Override
    public InventoryValuationView valuation(Long companyId, Long branchId) {
        List<ProductValuationView> byProduct = lotRepository.valuationRows(companyId, branchId)
                .stream()
                .map(r -> new ProductValuationView(r.getProductId(), r.getProductName(),
                        r.getProductCode(),
                        r.getQuantity() == null ? 0 : r.getQuantity().intValue(),
                        r.getValue() == null ? BigDecimal.ZERO : r.getValue()))
                .toList();
        BigDecimal total = byProduct.stream().map(ProductValuationView::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalUnits = byProduct.stream().mapToInt(ProductValuationView::quantity).sum();
        return new InventoryValuationView(total, totalUnits, byProduct);
    }

    @Override
    public PageResult<PurchaseView> purchases(SearchPurchasesQuery c) {
        LocalDateTime from = c.from() == null ? null : c.from().atStartOfDay();
        LocalDateTime to = c.to() == null ? null : c.to().plusDays(1).atStartOfDay();
        Page<PurchaseRow> page = movementRepository.searchPurchases(c.companyId(), c.branchId(),
                from, to, Pages.request(c.page(), c.pageSize()));
        return Pages.result(page, r -> {
            BigDecimal unitCost = r.getUnitCost() == null ? BigDecimal.ZERO : r.getUnitCost();
            return new PurchaseView(r.getId(), r.getProductId(), r.getProductName(),
                    r.getProductCode(), r.getLotId(), r.getBranchId(), r.getBranchName(),
                    r.getQuantity(), unitCost,
                    unitCost.multiply(BigDecimal.valueOf(Math.abs(r.getQuantity()))),
                    r.getReferenceId(), r.getCreatedDate());
        });
    }

    @Override
    public List<KardexExportRow> kardexForExport(SearchKardexCommand c) {
        LocalDateTime from = c.from() == null ? null : c.from().atStartOfDay();
        LocalDateTime to = c.to() == null ? null : c.to().plusDays(1).atStartOfDay();
        return movementRepository
                .kardexForExport(c.companyId(), c.productId(), c.branchId(), from, to).stream()
                .map(r -> new KardexExportRow(r.getProductName(), r.getProductCode(),
                        r.getBranchName(), r.getCreatedDate(), r.getType(), r.getReferenceType(),
                        r.getReferenceId(), r.getLotId(), r.getQuantity(),
                        r.getUnitCost() == null ? BigDecimal.ZERO : r.getUnitCost()))
                .toList();
    }

    @Override
    public int openingBalance(Long companyId, Long productId, Long branchId, LocalDate from) {
        return movementRepository.sumQuantityBefore(companyId, productId, branchId,
                from.atStartOfDay());
    }

    @Override
    public List<PurchaseView> purchasesForExport(SearchPurchasesQuery c) {
        LocalDateTime from = c.from() == null ? null : c.from().atStartOfDay();
        LocalDateTime to = c.to() == null ? null : c.to().plusDays(1).atStartOfDay();
        return movementRepository.purchasesForExport(c.companyId(), c.branchId(), from, to).stream()
                .map(r -> {
                    BigDecimal unitCost = r.getUnitCost() == null
                            ? BigDecimal.ZERO
                            : r.getUnitCost();
                    return new PurchaseView(r.getId(), r.getProductId(), r.getProductName(),
                            r.getProductCode(), r.getLotId(), r.getBranchId(), r.getBranchName(),
                            r.getQuantity(), unitCost,
                            unitCost.multiply(BigDecimal.valueOf(Math.abs(r.getQuantity()))),
                            r.getReferenceId(), r.getCreatedDate());
                }).toList();
    }

    private static StockView toStockView(StockRow r) {
        return new StockView(r.getProductId(), r.getProductName(), r.getProductCode(),
                r.getBranchId(), r.getBranchName(), r.getQuantity(), r.getMinStock(),
                r.getQuantity() < r.getMinStock());
    }
}
