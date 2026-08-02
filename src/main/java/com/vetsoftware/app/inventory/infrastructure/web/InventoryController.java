package com.vetsoftware.app.inventory.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.inventory.application.command.InventoryAlertsQuery;
import com.vetsoftware.app.inventory.application.command.InventoryValuationQuery;
import com.vetsoftware.app.inventory.application.command.ListLotsCommand;
import com.vetsoftware.app.inventory.application.command.RecordAdjustmentCommand;
import com.vetsoftware.app.inventory.application.command.RecordClinicalUseCommand;
import com.vetsoftware.app.inventory.application.command.RecordCountCommand;
import com.vetsoftware.app.inventory.application.command.RecordPurchaseCommand;
import com.vetsoftware.app.inventory.application.command.SearchCountsQuery;
import com.vetsoftware.app.inventory.application.command.SearchKardexCommand;
import com.vetsoftware.app.inventory.application.command.SearchPurchasesQuery;
import com.vetsoftware.app.inventory.application.command.SearchStockCommand;
import com.vetsoftware.app.inventory.application.command.SetMinStockCommand;
import com.vetsoftware.app.inventory.application.command.TransferStockCommand;
import com.vetsoftware.app.inventory.application.dto.InventoryAlertsView;
import com.vetsoftware.app.inventory.application.dto.InventoryCountView;
import com.vetsoftware.app.inventory.application.dto.InventoryValuationView;
import com.vetsoftware.app.inventory.application.dto.KardexReport;
import com.vetsoftware.app.inventory.application.dto.PageResult;
import com.vetsoftware.app.inventory.application.dto.PurchaseView;
import com.vetsoftware.app.inventory.application.dto.PurchasesReport;
import com.vetsoftware.app.inventory.application.dto.StockLotView;
import com.vetsoftware.app.inventory.application.dto.StockMovementView;
import com.vetsoftware.app.inventory.application.dto.StockView;
import com.vetsoftware.app.inventory.application.port.in.AdjustStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.ConsumeStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.ExportInventoryUseCase;
import com.vetsoftware.app.inventory.application.port.in.GetCountUseCase;
import com.vetsoftware.app.inventory.application.port.in.GetInventoryAlertsUseCase;
import com.vetsoftware.app.inventory.application.port.in.GetInventoryValuationUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListCountsUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListKardexUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListProductLotsUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListPurchasesUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.ReceiveStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.RecordCountUseCase;
import com.vetsoftware.app.inventory.application.port.in.SetMinStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.TransferStockUseCase;
import com.vetsoftware.app.inventory.application.port.out.InventoryReportPdfPort;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import com.vetsoftware.app.inventory.infrastructure.web.request.AdjustStockRequest;
import com.vetsoftware.app.inventory.infrastructure.web.request.ConsumeStockRequest;
import com.vetsoftware.app.inventory.infrastructure.web.request.ReceiveStockRequest;
import com.vetsoftware.app.inventory.infrastructure.web.request.RecordCountRequest;
import com.vetsoftware.app.inventory.infrastructure.web.request.SetMinStockRequest;
import com.vetsoftware.app.inventory.infrastructure.web.request.TransferStockRequest;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Inventario por sede (F3). Lecturas con {@code inventory.read};
 * ajustes/entradas/mínimo con {@code
 * inventory.adjust}; transferencias con {@code inventory.transfer} (gate en
 * los @PreAuthorize de los puertos). La sede se resuelve por el alcance del
 * empleado con {@link Authz#resolveAccessibleBranch}: mostrador ve/opera su(s)
 * sede(s), admin todas.
 */
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final ListStockUseCase listStockUseCase;
    private final ListProductLotsUseCase listLotsUseCase;
    private final ListKardexUseCase listKardexUseCase;
    private final AdjustStockUseCase adjustStockUseCase;
    private final ReceiveStockUseCase receiveStockUseCase;
    private final TransferStockUseCase transferStockUseCase;
    private final SetMinStockUseCase setMinStockUseCase;
    private final ConsumeStockUseCase consumeStockUseCase;
    private final GetInventoryAlertsUseCase alertsUseCase;
    private final GetInventoryValuationUseCase valuationUseCase;
    private final ListPurchasesUseCase listPurchasesUseCase;
    private final RecordCountUseCase recordCountUseCase;
    private final ListCountsUseCase listCountsUseCase;
    private final GetCountUseCase getCountUseCase;
    private final ExportInventoryUseCase exportInventoryUseCase;
    private final InventoryReportPdfPort inventoryReportPdf;
    private final Authz authz;

    public InventoryController(ListStockUseCase listStockUseCase,
            ListProductLotsUseCase listLotsUseCase, ListKardexUseCase listKardexUseCase,
            AdjustStockUseCase adjustStockUseCase, ReceiveStockUseCase receiveStockUseCase,
            TransferStockUseCase transferStockUseCase, SetMinStockUseCase setMinStockUseCase,
            ConsumeStockUseCase consumeStockUseCase, GetInventoryAlertsUseCase alertsUseCase,
            GetInventoryValuationUseCase valuationUseCase,
            ListPurchasesUseCase listPurchasesUseCase, RecordCountUseCase recordCountUseCase,
            ListCountsUseCase listCountsUseCase, GetCountUseCase getCountUseCase,
            ExportInventoryUseCase exportInventoryUseCase,
            InventoryReportPdfPort inventoryReportPdf, Authz authz) {
        this.listStockUseCase = listStockUseCase;
        this.listLotsUseCase = listLotsUseCase;
        this.listKardexUseCase = listKardexUseCase;
        this.adjustStockUseCase = adjustStockUseCase;
        this.receiveStockUseCase = receiveStockUseCase;
        this.transferStockUseCase = transferStockUseCase;
        this.setMinStockUseCase = setMinStockUseCase;
        this.consumeStockUseCase = consumeStockUseCase;
        this.alertsUseCase = alertsUseCase;
        this.valuationUseCase = valuationUseCase;
        this.listPurchasesUseCase = listPurchasesUseCase;
        this.recordCountUseCase = recordCountUseCase;
        this.listCountsUseCase = listCountsUseCase;
        this.getCountUseCase = getCountUseCase;
        this.exportInventoryUseCase = exportInventoryUseCase;
        this.inventoryReportPdf = inventoryReportPdf;
        this.authz = authz;
    }

    @GetMapping("/stock")
    public PageResponse<StockView> stock(@RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean lowStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<StockView> result = listStockUseCase
                .search(new SearchStockCommand(authz.currentCompanyId(),
                        authz.resolveAccessibleBranch(branchId), q, lowStock, page, pageSize));
        return toPageResponse(result);
    }

    @GetMapping("/products/{productId}/lots")
    public List<StockLotView> lots(@PathVariable Long productId,
            @RequestParam(required = false) Long branchId) {
        return listLotsUseCase.listLots(new ListLotsCommand(authz.currentCompanyId(),
                authz.resolveAccessibleBranch(branchId), productId));
    }

    @GetMapping("/products/{productId}/kardex")
    public PageResponse<StockMovementView> kardex(@PathVariable Long productId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<StockMovementView> result = listKardexUseCase
                .searchKardex(new SearchKardexCommand(authz.currentCompanyId(),
                        authz.resolveAccessibleBranch(branchId), productId, from, to, page,
                        pageSize));
        return toPageResponse(result);
    }

    @GetMapping("/alerts")
    public InventoryAlertsView alerts(@RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "30") int expiringInDays) {
        return alertsUseCase.alerts(new InventoryAlertsQuery(authz.currentCompanyId(),
                authz.resolveAccessibleBranch(branchId), expiringInDays));
    }

    @GetMapping("/valuation")
    public InventoryValuationView valuation(@RequestParam(required = false) Long branchId) {
        return valuationUseCase.valuation(new InventoryValuationQuery(authz.currentCompanyId(),
                authz.resolveAccessibleBranch(branchId)));
    }

    @GetMapping("/purchases")
    public PageResponse<PurchaseView> purchases(@RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<PurchaseView> result = listPurchasesUseCase
                .purchases(new SearchPurchasesQuery(authz.currentCompanyId(),
                        authz.resolveAccessibleBranch(branchId), from, to, page, pageSize));
        return toPageResponse(result);
    }

    @PostMapping("/consumptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void consume(@Valid @RequestBody ConsumeStockRequest request) {
        consumeStockUseCase.consume(new RecordClinicalUseCommand(authz.currentCompanyId(),
                authz.resolveAccessibleBranch(request.branchId()), request.productId(),
                request.quantity(), null, request.reason(), authz.currentEmployeeIdOrNull()));
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adjust(@Valid @RequestBody AdjustStockRequest request) {
        adjustStockUseCase.adjust(new RecordAdjustmentCommand(authz.currentCompanyId(),
                authz.resolveAccessibleBranch(request.branchId()), request.productId(),
                request.delta(), request.unitCost(), request.reason(), null,
                authz.currentEmployeeIdOrNull()));
    }

    @PostMapping("/receipts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receive(@Valid @RequestBody ReceiveStockRequest request) {
        receiveStockUseCase.receive(new RecordPurchaseCommand(authz.currentCompanyId(),
                authz.resolveAccessibleBranch(request.branchId()), request.productId(),
                request.lotNumber(), request.expireDate(), request.quantity(), request.unitCost(),
                StockReferenceType.GOODS_RECEIPT, null, authz.currentEmployeeIdOrNull()));
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transfer(@Valid @RequestBody TransferStockRequest request) {
        transferStockUseCase.transfer(new TransferStockCommand(authz.currentCompanyId(),
                // Origen: acotado al alcance del empleado. Destino: cualquier sede de la
                // empresa
                // (validada en el service).
                authz.resolveAccessibleBranch(request.fromBranchId()), request.toBranchId(),
                request.productId(), request.quantity(), request.reason(),
                authz.currentEmployeeIdOrNull()));
    }

    @PutMapping("/products/{productId}/min-stock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setMinStock(@PathVariable Long productId,
            @Valid @RequestBody SetMinStockRequest request) {
        setMinStockUseCase.setMinStock(new SetMinStockCommand(authz.currentCompanyId(),
                authz.resolveAccessibleBranch(request.branchId()), productId, request.minStock()));
    }

    // Conteo físico (cíclico): confirma la hoja de conteo y devuelve la sesión con
    // las diferencias
    // aplicadas.
    @PostMapping("/counts")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryCountView recordCount(@Valid @RequestBody RecordCountRequest request) {
        return recordCountUseCase.record(new RecordCountCommand(authz.currentCompanyId(),
                authz.resolveAccessibleBranch(request.branchId()), request.note(),
                authz.currentEmployeeIdOrNull(),
                request.lines().stream()
                        .map(l -> new RecordCountCommand.Line(l.productId(), l.countedQuantity()))
                        .toList()));
    }

    @GetMapping("/counts")
    public PageResponse<InventoryCountView> counts(@RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<InventoryCountView> result = listCountsUseCase.list(new SearchCountsQuery(
                authz.currentCompanyId(), authz.resolveAccessibleBranch(branchId), page, pageSize));
        return toPageResponse(result);
    }

    @GetMapping("/counts/{id}")
    public InventoryCountView count(@PathVariable Long id) {
        return getCountUseCase.get(authz.currentCompanyId(), id);
    }

    // ── Exportación (CSV/PDF)
    // ─────────────────────────────────────────────────────
    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    @GetMapping("/products/{productId}/kardex/export")
    public ResponseEntity<byte[]> exportKardex(@PathVariable Long productId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "csv") String format) {
        KardexReport report = exportInventoryUseCase
                .kardexReport(new SearchKardexCommand(authz.currentCompanyId(),
                        authz.resolveAccessibleBranch(branchId), productId, from, to, 0, 0));
        String base = "kardex_" + productId;
        return "pdf".equalsIgnoreCase(format)
                ? file(inventoryReportPdf.renderKardex(report), base + ".pdf",
                        MediaType.APPLICATION_PDF)
                : file(InventoryCsv.kardex(report), base + ".csv", CSV);
    }

    @GetMapping("/purchases/export")
    public ResponseEntity<byte[]> exportPurchases(@RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "csv") String format) {
        PurchasesReport report = exportInventoryUseCase.purchasesReport(new SearchPurchasesQuery(
                authz.currentCompanyId(), authz.resolveAccessibleBranch(branchId), from, to, 0, 0));
        return "pdf".equalsIgnoreCase(format)
                ? file(inventoryReportPdf.renderPurchases(report), "compras.pdf",
                        MediaType.APPLICATION_PDF)
                : file(InventoryCsv.purchases(report), "compras.csv", CSV);
    }

    private static ResponseEntity<byte[]> file(byte[] body, String filename, MediaType type) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
                .body(body);
    }

    private static <T> PageResponse<T> toPageResponse(PageResult<T> r) {
        return new PageResponse<>(r.content(), r.page(), r.pageSize(), r.totalElements(),
                r.totalPages());
    }
}
