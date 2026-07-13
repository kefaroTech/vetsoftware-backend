package com.vetsoftware.app.supplierinvoice.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.supplierinvoice.application.command.CreateSupplierInvoiceCommand;
import com.vetsoftware.app.supplierinvoice.application.command.RegisterSupplierPaymentCommand;
import com.vetsoftware.app.supplierinvoice.application.command.SearchSupplierInvoicesCommand;
import com.vetsoftware.app.supplierinvoice.application.command.UpdateSupplierInvoiceCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.AccountsPayableAgingDto;
import com.vetsoftware.app.supplierinvoice.application.dto.PageResult;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoicePaymentDto;
import com.vetsoftware.app.supplierinvoice.application.port.in.CancelSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.CreateSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.DeleteSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.FindSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.GetAccountsPayableAgingUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.RegisterSupplierPaymentUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.SearchSupplierInvoicesUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.UpdateSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import com.vetsoftware.app.supplierinvoice.infrastructure.web.request.CreateSupplierInvoiceRequest;
import com.vetsoftware.app.supplierinvoice.infrastructure.web.request.RegisterSupplierPaymentRequest;
import com.vetsoftware.app.supplierinvoice.infrastructure.web.request.UpdateSupplierInvoiceRequest;
import com.vetsoftware.app.supplierinvoice.infrastructure.web.response.AccountsPayableAgingResponse;
import com.vetsoftware.app.supplierinvoice.infrastructure.web.response.BranchSummary;
import com.vetsoftware.app.supplierinvoice.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.supplierinvoice.infrastructure.web.response.SupplierInvoicePaymentResponse;
import com.vetsoftware.app.supplierinvoice.infrastructure.web.response.SupplierInvoiceResponse;
import com.vetsoftware.app.supplierinvoice.infrastructure.web.response.SupplierSummary;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Facturas de proveedor y cuentas por pagar (punto 7 / F3). CRUD company-scoped + abonos parciales + reporte de
 * antigüedad (aging). La sede se resuelve por el alcance del empleado ({@link Authz#resolveAccessibleBranch}).
 */
@RestController
@RequestMapping("/supplier-invoices")
public class SupplierInvoiceController {
    private final CreateSupplierInvoiceUseCase createUseCase;
    private final UpdateSupplierInvoiceUseCase updateUseCase;
    private final FindSupplierInvoiceUseCase findUseCase;
    private final SearchSupplierInvoicesUseCase searchUseCase;
    private final RegisterSupplierPaymentUseCase registerPaymentUseCase;
    private final CancelSupplierInvoiceUseCase cancelUseCase;
    private final DeleteSupplierInvoiceUseCase deleteUseCase;
    private final GetAccountsPayableAgingUseCase agingUseCase;
    private final Authz authz;

    public SupplierInvoiceController(CreateSupplierInvoiceUseCase createUseCase,
                                     UpdateSupplierInvoiceUseCase updateUseCase,
                                     FindSupplierInvoiceUseCase findUseCase,
                                     SearchSupplierInvoicesUseCase searchUseCase,
                                     RegisterSupplierPaymentUseCase registerPaymentUseCase,
                                     CancelSupplierInvoiceUseCase cancelUseCase,
                                     DeleteSupplierInvoiceUseCase deleteUseCase,
                                     GetAccountsPayableAgingUseCase agingUseCase,
                                     Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.searchUseCase = searchUseCase;
        this.registerPaymentUseCase = registerPaymentUseCase;
        this.cancelUseCase = cancelUseCase;
        this.deleteUseCase = deleteUseCase;
        this.agingUseCase = agingUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierInvoiceResponse create(@Valid @RequestBody CreateSupplierInvoiceRequest request) {
        return toResponse(createUseCase.execute(new CreateSupplierInvoiceCommand(
            authz.resolveAccessibleBranch(request.branchId()), request.supplierId(), request.purchaseOrderId(),
            request.goodsReceiptId(), request.invoiceNumber(), request.issueDate(), request.dueDate(),
            request.subtotal(), request.taxAmount(), request.withholdingAmount(), request.notes(),
            authz.currentCompanyId(), authz.currentEmployeeIdOrNull())));
    }

    @GetMapping("/search")
    public PageResponse<SupplierInvoiceResponse> search(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) SupplierInvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<SupplierInvoiceDto> result = searchUseCase.execute(new SearchSupplierInvoicesCommand(
            authz.currentCompanyId(), supplierId, authz.resolveAccessibleBranch(branchId), status, from, to,
            page, pageSize));
        return new PageResponse<>(
            result.content().stream().map(this::toResponse).toList(),
            result.page(), result.pageSize(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/aging")
    public AccountsPayableAgingResponse aging(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return toAgingResponse(agingUseCase.get(
            authz.currentCompanyId(), authz.resolveAccessibleBranch(branchId), asOf));
    }

    @GetMapping("/{id}")
    public SupplierInvoiceResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public SupplierInvoiceResponse update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateSupplierInvoiceRequest request) {
        return toResponse(updateUseCase.execute(new UpdateSupplierInvoiceCommand(
            id, authz.resolveAccessibleBranch(request.branchId()), request.supplierId(), request.purchaseOrderId(),
            request.goodsReceiptId(), request.invoiceNumber(), request.issueDate(), request.dueDate(),
            request.subtotal(), request.taxAmount(), request.withholdingAmount(), request.notes(),
            authz.currentCompanyId(), authz.currentEmployeeIdOrNull(), request.version())));
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierInvoiceResponse registerPayment(@PathVariable Long id,
                                                   @Valid @RequestBody RegisterSupplierPaymentRequest request) {
        return toResponse(registerPaymentUseCase.execute(new RegisterSupplierPaymentCommand(
            id, request.amount(), request.paymentDate(), request.method(), request.reference(), request.note(),
            authz.currentCompanyId(), authz.currentEmployeeIdOrNull(), request.version())));
    }

    @PostMapping("/{id}/cancel")
    public SupplierInvoiceResponse cancel(@PathVariable Long id) {
        return toResponse(cancelUseCase.execute(id, authz.currentCompanyId(), authz.currentEmployeeIdOrNull()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    private SupplierInvoiceResponse toResponse(SupplierInvoiceDto d) {
        return new SupplierInvoiceResponse(
            d.id(),
            new CompanySummary(d.company().id(), d.company().name(), d.company().identifier()),
            new BranchSummary(d.branch().id(), d.branch().name()),
            new SupplierSummary(d.supplier().id(), d.supplier().name(), d.supplier().taxId()),
            d.purchaseOrderId(), d.goodsReceiptId(), d.invoiceNumber(), d.issueDate(), d.dueDate(),
            d.subtotal(), d.taxAmount(), d.withholdingAmount(), d.total(), d.payableAmount(), d.paidAmount(),
            d.balance(), d.status(), d.notes(),
            d.payments().stream().map(this::toPaymentResponse).toList(),
            d.createdDate(), d.createdBy(), d.updatedDate(), d.updatedBy(), d.version(), d.enabled());
    }

    private SupplierInvoicePaymentResponse toPaymentResponse(SupplierInvoicePaymentDto p) {
        return new SupplierInvoicePaymentResponse(p.id(), p.amount(), p.paymentDate(), p.method(),
            p.reference(), p.note(), p.createdDate(), p.createdBy());
    }

    private AccountsPayableAgingResponse toAgingResponse(AccountsPayableAgingDto d) {
        return new AccountsPayableAgingResponse(
            d.asOf(),
            d.suppliers().stream().map(s -> new AccountsPayableAgingResponse.SupplierRow(
                s.supplierId(), s.supplierName(), s.taxId(), toBucket(s.bucket()))).toList(),
            toBucket(d.totals()));
    }

    private AccountsPayableAgingResponse.Bucket toBucket(AccountsPayableAgingDto.Bucket b) {
        return new AccountsPayableAgingResponse.Bucket(
            b.current(), b.days1to30(), b.days31to60(), b.days61to90(), b.over90(), b.total());
    }
}
