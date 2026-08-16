package com.vetsoftware.app.supplier.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.supplier.application.command.CreateSupplierCommand;
import com.vetsoftware.app.supplier.application.command.SearchSuppliersCommand;
import com.vetsoftware.app.supplier.application.command.UpdateSupplierCommand;
import com.vetsoftware.app.supplier.application.dto.CompanySummaryDto;
import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.in.CreateSupplierUseCase;
import com.vetsoftware.app.supplier.application.port.in.DeleteSupplierUseCase;
import com.vetsoftware.app.supplier.application.port.in.FindSupplierUseCase;
import com.vetsoftware.app.supplier.application.port.in.ListSuppliersUseCase;
import com.vetsoftware.app.supplier.application.port.in.ReactivateSupplierUseCase;
import com.vetsoftware.app.supplier.application.port.in.SearchSuppliersUseCase;
import com.vetsoftware.app.supplier.application.port.in.UpdateSupplierUseCase;
import com.vetsoftware.app.supplier.infrastructure.web.request.CreateSupplierRequest;
import com.vetsoftware.app.supplier.infrastructure.web.request.UpdateSupplierRequest;
import com.vetsoftware.app.supplier.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.supplier.infrastructure.web.response.SupplierResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {
    private final CreateSupplierUseCase createUseCase;
    private final UpdateSupplierUseCase updateUseCase;
    private final FindSupplierUseCase findUseCase;
    private final ListSuppliersUseCase listUseCase;
    private final SearchSuppliersUseCase searchUseCase;
    private final DeleteSupplierUseCase deleteUseCase;
    private final ReactivateSupplierUseCase reactivateUseCase;
    private final Authz authz;

    public SupplierController(CreateSupplierUseCase createUseCase,
            UpdateSupplierUseCase updateUseCase, FindSupplierUseCase findUseCase,
            ListSuppliersUseCase listUseCase, SearchSuppliersUseCase searchUseCase,
            DeleteSupplierUseCase deleteUseCase, ReactivateSupplierUseCase reactivateUseCase,
            Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.searchUseCase = searchUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody CreateSupplierRequest request) {
        return toResponse(
                createUseCase.execute(new CreateSupplierCommand(request.name(), request.taxId(),
                        request.contactName(), request.phone(), request.email(), request.address(),
                        request.paymentTermsDays(), request.notes(), authz.currentCompanyId())));
    }

    @GetMapping
    public List<SupplierResponse> listByCompany() {
        return listUseCase.listByCompany(authz.currentCompanyId()).stream().map(this::toResponse)
                .toList();
    }

    @GetMapping("/disabled")
    public List<SupplierResponse> listDisabled() {
        return listUseCase.listDisabledByCompany(authz.currentCompanyId()).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/search")
    public PageResponse<SupplierResponse> search(@RequestParam(required = false) String name,
            @RequestParam(required = false) String taxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(searchUseCase.execute(
                new SearchSuppliersCommand(authz.currentCompanyId(), name, taxId, page, pageSize)),
                this::toResponse);
    }

    @GetMapping("/{id}")
    public SupplierResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateSupplierRequest request) {
        return toResponse(updateUseCase.execute(new UpdateSupplierCommand(id, request.name(),
                request.taxId(), request.contactName(), request.phone(), request.email(),
                request.address(), request.paymentTermsDays(), request.notes(),
                authz.currentCompanyId(), authz.currentEmployeeIdOrNull(), request.version())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    @PatchMapping("/{id}/enable")
    public SupplierResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyId()));
    }

    private SupplierResponse toResponse(SupplierDto dto) {
        CompanySummaryDto c = dto.company();
        return new SupplierResponse(dto.id(), dto.name(), dto.taxId(), dto.contactName(),
                dto.phone(), dto.email(), dto.address(), dto.paymentTermsDays(), dto.notes(),
                new CompanySummary(c.id(), c.name(), c.identifier()), dto.createdDate(),
                dto.updatedDate(), dto.updatedBy(), dto.version(), dto.enabled());
    }
}
