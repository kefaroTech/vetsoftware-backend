package com.vetsoftware.app.goodsreceipt.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.goodsreceipt.application.command.CreateGoodsReceiptCommand;
import com.vetsoftware.app.goodsreceipt.application.command.GoodsReceiptLineCommand;
import com.vetsoftware.app.goodsreceipt.application.command.SearchGoodsReceiptsCommand;
import com.vetsoftware.app.goodsreceipt.application.dto.BranchSummaryDto;
import com.vetsoftware.app.goodsreceipt.application.dto.CompanySummaryDto;
import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptLineDto;
import com.vetsoftware.app.goodsreceipt.application.dto.SupplierSummaryDto;
import com.vetsoftware.app.goodsreceipt.application.port.in.CancelGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.in.ConfirmGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.in.CreateGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.in.DeleteGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.in.FindGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.in.SearchGoodsReceiptsUseCase;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.infrastructure.web.request.CreateGoodsReceiptRequest;
import com.vetsoftware.app.goodsreceipt.infrastructure.web.request.GoodsReceiptLineRequest;
import com.vetsoftware.app.goodsreceipt.infrastructure.web.response.BranchSummary;
import com.vetsoftware.app.goodsreceipt.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.goodsreceipt.infrastructure.web.response.GoodsReceiptLineResponse;
import com.vetsoftware.app.goodsreceipt.infrastructure.web.response.GoodsReceiptResponse;
import com.vetsoftware.app.goodsreceipt.infrastructure.web.response.ProductSummary;
import com.vetsoftware.app.goodsreceipt.infrastructure.web.response.SupplierSummary;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/goods-receipts")
public class GoodsReceiptController {
    private final CreateGoodsReceiptUseCase createUseCase;
    private final FindGoodsReceiptUseCase findUseCase;
    private final SearchGoodsReceiptsUseCase searchUseCase;
    private final DeleteGoodsReceiptUseCase deleteUseCase;
    private final ConfirmGoodsReceiptUseCase confirmUseCase;
    private final CancelGoodsReceiptUseCase cancelUseCase;
    private final Authz authz;

    public GoodsReceiptController(CreateGoodsReceiptUseCase createUseCase,
            FindGoodsReceiptUseCase findUseCase, SearchGoodsReceiptsUseCase searchUseCase,
            DeleteGoodsReceiptUseCase deleteUseCase, ConfirmGoodsReceiptUseCase confirmUseCase,
            CancelGoodsReceiptUseCase cancelUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.findUseCase = findUseCase;
        this.searchUseCase = searchUseCase;
        this.deleteUseCase = deleteUseCase;
        this.confirmUseCase = confirmUseCase;
        this.cancelUseCase = cancelUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoodsReceiptResponse create(@Valid @RequestBody CreateGoodsReceiptRequest request) {
        return toResponse(createUseCase.execute(new CreateGoodsReceiptCommand(request.branchId(),
                request.supplierId(), request.purchaseOrderId(), request.receiptDate(),
                request.supplierInvoiceNumber(), request.notes(), toLineCommands(request.lines()),
                authz.currentCompanyId(), authz.currentEmployeeIdOrNull())));
    }

    @GetMapping("/search")
    public PageResponse<GoodsReceiptResponse> search(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) GoodsReceiptStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse
                .from(searchUseCase.execute(new SearchGoodsReceiptsCommand(authz.currentCompanyId(),
                        supplierId, branchId, status, from, to, page, pageSize)), this::toResponse);
    }

    @GetMapping("/{id}")
    public GoodsReceiptResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    @PostMapping("/{id}/confirm")
    public GoodsReceiptResponse confirm(@PathVariable Long id) {
        return toResponse(confirmUseCase.execute(id, authz.currentCompanyId(),
                authz.currentEmployeeIdOrNull()));
    }

    @PostMapping("/{id}/cancel")
    public GoodsReceiptResponse cancel(@PathVariable Long id) {
        return toResponse(cancelUseCase.execute(id, authz.currentCompanyId(),
                authz.currentEmployeeIdOrNull()));
    }

    private List<GoodsReceiptLineCommand> toLineCommands(List<GoodsReceiptLineRequest> lines) {
        return lines.stream()
                .map(l -> new GoodsReceiptLineCommand(l.productId(), l.purchaseOrderLineId(),
                        l.lotNumber(), l.expireDate(), l.quantityReceived(), l.unitCost()))
                .toList();
    }

    private GoodsReceiptResponse toResponse(GoodsReceiptDto dto) {
        CompanySummaryDto c = dto.company();
        BranchSummaryDto b = dto.branch();
        SupplierSummaryDto s = dto.supplier();
        List<GoodsReceiptLineResponse> lines = dto.lines().stream().map(this::toLineResponse)
                .toList();
        return new GoodsReceiptResponse(dto.id(),
                new CompanySummary(c.id(), c.name(), c.identifier()),
                new BranchSummary(b.id(), b.name()), new SupplierSummary(s.id(), s.name()),
                dto.purchaseOrderId(), dto.receiptDate(), dto.supplierInvoiceNumber(), dto.notes(),
                dto.status(), lines, dto.createdDate(), dto.createdBy(), dto.updatedDate(),
                dto.updatedBy(), dto.version(), dto.enabled());
    }

    private GoodsReceiptLineResponse toLineResponse(GoodsReceiptLineDto l) {
        return new GoodsReceiptLineResponse(l.id(),
                new ProductSummary(l.product().id(), l.product().name(), l.product().code()),
                l.purchaseOrderLineId(), l.lotNumber(), l.expireDate(), l.quantityReceived(),
                l.unitCost());
    }
}
