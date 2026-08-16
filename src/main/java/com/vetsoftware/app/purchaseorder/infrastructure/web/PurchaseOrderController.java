package com.vetsoftware.app.purchaseorder.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.purchaseorder.application.command.CreatePurchaseOrderCommand;
import com.vetsoftware.app.purchaseorder.application.command.PurchaseOrderLineCommand;
import com.vetsoftware.app.purchaseorder.application.command.SearchPurchaseOrdersCommand;
import com.vetsoftware.app.purchaseorder.application.command.UpdatePurchaseOrderCommand;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderLineDto;
import com.vetsoftware.app.purchaseorder.application.port.in.CancelPurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.CreatePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.DeletePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.FindPurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.ListPurchaseOrdersUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.PlacePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.ReactivatePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.SearchPurchaseOrdersUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.UpdatePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.purchaseorder.infrastructure.web.request.CreatePurchaseOrderRequest;
import com.vetsoftware.app.purchaseorder.infrastructure.web.request.PurchaseOrderLineRequest;
import com.vetsoftware.app.purchaseorder.infrastructure.web.request.UpdatePurchaseOrderRequest;
import com.vetsoftware.app.purchaseorder.infrastructure.web.response.BranchSummary;
import com.vetsoftware.app.purchaseorder.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.purchaseorder.infrastructure.web.response.ProductSummary;
import com.vetsoftware.app.purchaseorder.infrastructure.web.response.PurchaseOrderLineResponse;
import com.vetsoftware.app.purchaseorder.infrastructure.web.response.PurchaseOrderResponse;
import com.vetsoftware.app.purchaseorder.infrastructure.web.response.SupplierSummary;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {
    private final CreatePurchaseOrderUseCase createUseCase;
    private final UpdatePurchaseOrderUseCase updateUseCase;
    private final FindPurchaseOrderUseCase findUseCase;
    private final ListPurchaseOrdersUseCase listUseCase;
    private final SearchPurchaseOrdersUseCase searchUseCase;
    private final DeletePurchaseOrderUseCase deleteUseCase;
    private final ReactivatePurchaseOrderUseCase reactivateUseCase;
    private final PlacePurchaseOrderUseCase placeUseCase;
    private final CancelPurchaseOrderUseCase cancelUseCase;
    private final Authz authz;

    public PurchaseOrderController(CreatePurchaseOrderUseCase createUseCase,
            UpdatePurchaseOrderUseCase updateUseCase, FindPurchaseOrderUseCase findUseCase,
            ListPurchaseOrdersUseCase listUseCase, SearchPurchaseOrdersUseCase searchUseCase,
            DeletePurchaseOrderUseCase deleteUseCase,
            ReactivatePurchaseOrderUseCase reactivateUseCase,
            PlacePurchaseOrderUseCase placeUseCase, CancelPurchaseOrderUseCase cancelUseCase,
            Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.searchUseCase = searchUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.placeUseCase = placeUseCase;
        this.cancelUseCase = cancelUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponse create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        return toResponse(createUseCase.execute(new CreatePurchaseOrderCommand(request.branchId(),
                request.supplierId(), request.orderDate(), request.expectedDate(), request.notes(),
                toLineCommands(request.lines()), authz.currentCompanyId(),
                authz.currentEmployeeIdOrNull())));
    }

    @GetMapping
    public List<PurchaseOrderResponse> listByCompany() {
        return listUseCase.listByCompany(authz.currentCompanyId()).stream().map(this::toResponse)
                .toList();
    }

    @GetMapping("/disabled")
    public List<PurchaseOrderResponse> listDisabled() {
        return listUseCase.listDisabledByCompany(authz.currentCompanyId()).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/search")
    public PageResponse<PurchaseOrderResponse> search(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<PurchaseOrderDto> result = searchUseCase.execute(new SearchPurchaseOrdersCommand(
                authz.currentCompanyId(), supplierId, branchId, status, from, to, page, pageSize));
        return PageResponse.from(result, this::toResponse);
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public PurchaseOrderResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdatePurchaseOrderRequest request) {
        return toResponse(updateUseCase.execute(new UpdatePurchaseOrderCommand(id,
                request.branchId(), request.supplierId(), request.orderDate(),
                request.expectedDate(), request.notes(), toLineCommands(request.lines()),
                authz.currentCompanyId(), authz.currentEmployeeIdOrNull(), request.version())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    @PatchMapping("/{id}/enable")
    public PurchaseOrderResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyId()));
    }

    @PostMapping("/{id}/place")
    public PurchaseOrderResponse place(@PathVariable Long id) {
        return toResponse(placeUseCase.execute(id, authz.currentCompanyId(),
                authz.currentEmployeeIdOrNull()));
    }

    @PostMapping("/{id}/cancel")
    public PurchaseOrderResponse cancel(@PathVariable Long id) {
        return toResponse(cancelUseCase.execute(id, authz.currentCompanyId(),
                authz.currentEmployeeIdOrNull()));
    }

    private List<PurchaseOrderLineCommand> toLineCommands(List<PurchaseOrderLineRequest> lines) {
        if (lines == null)
            return List.of();
        return lines.stream().map(
                l -> new PurchaseOrderLineCommand(l.productId(), l.quantityOrdered(), l.unitCost()))
                .toList();
    }

    private PurchaseOrderResponse toResponse(PurchaseOrderDto dto) {
        return new PurchaseOrderResponse(dto.id(),
                new CompanySummary(dto.company().id(), dto.company().name(),
                        dto.company().identifier()),
                new BranchSummary(dto.branch().id(), dto.branch().name()),
                new SupplierSummary(dto.supplier().id(), dto.supplier().name()), dto.status(),
                dto.orderDate(), dto.expectedDate(), dto.notes(),
                dto.lines().stream().map(this::toLineResponse).toList(), dto.createdDate(),
                dto.createdBy(), dto.updatedDate(), dto.updatedBy(), dto.version(), dto.enabled());
    }

    private PurchaseOrderLineResponse toLineResponse(PurchaseOrderLineDto line) {
        return new PurchaseOrderLineResponse(line.id(),
                new ProductSummary(line.product().id(), line.product().name(),
                        line.product().code()),
                line.quantityOrdered(), line.unitCost(), line.quantityReceived(),
                line.pendingQuantity(), line.fullyReceived());
    }
}
