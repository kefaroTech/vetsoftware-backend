package com.vetsoftware.app.inventory.application.usecase;

import com.vetsoftware.app.inventory.application.command.SearchKardexCommand;
import com.vetsoftware.app.inventory.application.command.SearchPurchasesQuery;
import com.vetsoftware.app.inventory.application.dto.KardexExportRow;
import com.vetsoftware.app.inventory.application.dto.KardexReport;
import com.vetsoftware.app.inventory.application.dto.KardexReportLine;
import com.vetsoftware.app.inventory.application.dto.PurchaseView;
import com.vetsoftware.app.inventory.application.dto.PurchasesReport;
import com.vetsoftware.app.inventory.application.port.in.ExportInventoryUseCase;
import com.vetsoftware.app.inventory.application.port.out.StockQueryPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Construye los modelos de reporte de inventario. El kardex se recorre en orden ascendente arrastrando el saldo
 * corrido (partiendo del saldo previo a {@code from} si hay filtro de fecha); las compras se suman en totales.
 * Las etiquetas de tipo/referencia se traducen a ES aquí para que CSV y PDF compartan la misma nomenclatura.
 */
@Service
public class InventoryReportService implements ExportInventoryUseCase {

    private final StockQueryPort stockQueryPort;

    public InventoryReportService(StockQueryPort stockQueryPort) {
        this.stockQueryPort = stockQueryPort;
    }

    @Override
    public KardexReport kardexReport(SearchKardexCommand command) {
        List<KardexExportRow> rows = stockQueryPort.kardexForExport(command);
        int opening = command.from() == null ? 0
            : stockQueryPort.openingBalance(command.companyId(), command.productId(), command.branchId(),
                command.from());
        int running = opening;
        List<KardexReportLine> lines = new ArrayList<>(rows.size());
        for (KardexExportRow r : rows) {
            running += r.quantity();
            lines.add(new KardexReportLine(r.createdDate(), typeLabel(r.type()),
                referenceLabel(r.referenceType(), r.referenceId()), r.lotId(), r.quantity(), r.unitCost(), running));
        }
        String productName = rows.isEmpty() ? null : rows.get(0).productName();
        String productCode = rows.isEmpty() ? null : rows.get(0).productCode();
        String branchName = command.branchId() == null ? "Todas las sedes"
            : (rows.isEmpty() ? null : rows.get(0).branchName());
        return new KardexReport(productName, productCode, branchName, command.from(), command.to(),
            LocalDateTime.now(), opening, running, lines);
    }

    @Override
    public PurchasesReport purchasesReport(SearchPurchasesQuery query) {
        List<PurchaseView> lines = stockQueryPort.purchasesForExport(query);
        int totalQuantity = lines.stream().mapToInt(l -> Math.abs(l.quantity())).sum();
        BigDecimal totalValue = lines.stream().map(PurchaseView::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        String branchName = query.branchId() == null ? "Todas las sedes"
            : (lines.isEmpty() ? null : lines.get(0).branchName());
        return new PurchasesReport(branchName, query.from(), query.to(), LocalDateTime.now(), lines,
            totalQuantity, totalValue);
    }

    private static String typeLabel(String type) {
        if (type == null) return "";
        return switch (type) {
            case "PURCHASE" -> "Compra";
            case "SALE" -> "Venta";
            case "ADJUSTMENT_IN" -> "Ajuste entrada";
            case "ADJUSTMENT_OUT" -> "Ajuste salida";
            case "CLINICAL_USE" -> "Consumo clínico";
            case "TRANSFER_OUT" -> "Transferencia salida";
            case "TRANSFER_IN" -> "Transferencia entrada";
            case "VOID_IN" -> "Reversa entrada";
            case "VOID_OUT" -> "Reversa salida";
            default -> type;
        };
    }

    private static String referenceLabel(String referenceType, Long referenceId) {
        String base = referenceType == null ? "" : switch (referenceType) {
            case "POS_DOCUMENT" -> "Venta POS";
            case "OPEN_ACCOUNT_CHARGE" -> "Cuenta abierta";
            case "GOODS_RECEIPT" -> "Recepción";
            case "ADJUSTMENT" -> "Ajuste";
            case "TRANSFER" -> "Transferencia";
            case "CLINICAL_EVENT" -> "Evento clínico";
            default -> referenceType;
        };
        return referenceId == null ? base : base + " #" + referenceId;
    }
}
