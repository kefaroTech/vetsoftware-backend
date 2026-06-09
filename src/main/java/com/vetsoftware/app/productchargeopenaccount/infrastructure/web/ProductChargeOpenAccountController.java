package com.vetsoftware.app.productchargeopenaccount.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.productchargeopenaccount.application.command.CreateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.command.UpdateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.command.VoidProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.productchargeopenaccount.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.productchargeopenaccount.application.dto.OpenAccountSummaryDto;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductSummaryDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.CreateProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.DeleteProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.FindProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.ListProductChargeOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.ListProductChargeOpenAccountsUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.ReactivateProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.UpdateProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.VoidProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.infrastructure.web.request.CreateProductChargeOpenAccountRequest;
import com.vetsoftware.app.productchargeopenaccount.infrastructure.web.request.UpdateProductChargeOpenAccountRequest;
import com.vetsoftware.app.productchargeopenaccount.infrastructure.web.request.VoidProductChargeOpenAccountRequest;
import com.vetsoftware.app.productchargeopenaccount.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.productchargeopenaccount.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.productchargeopenaccount.infrastructure.web.response.OpenAccountSummary;
import com.vetsoftware.app.productchargeopenaccount.infrastructure.web.response.ProductChargeOpenAccountResponse;
import com.vetsoftware.app.productchargeopenaccount.infrastructure.web.response.ProductSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product-charge-open-accounts")
public class ProductChargeOpenAccountController {
    private final CreateProductChargeOpenAccountUseCase createUseCase;
    private final UpdateProductChargeOpenAccountUseCase updateUseCase;
    private final FindProductChargeOpenAccountUseCase findUseCase;
    private final ListProductChargeOpenAccountsUseCase listUseCase;
    private final ListProductChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase;
    private final DeleteProductChargeOpenAccountUseCase deleteUseCase;
    private final ReactivateProductChargeOpenAccountUseCase reactivateUseCase;
    private final VoidProductChargeOpenAccountUseCase voidUseCase;
    private final Authz authz;

    public ProductChargeOpenAccountController(CreateProductChargeOpenAccountUseCase createUseCase,
                                              UpdateProductChargeOpenAccountUseCase updateUseCase,
                                              FindProductChargeOpenAccountUseCase findUseCase,
                                              ListProductChargeOpenAccountsUseCase listUseCase,
                                              ListProductChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase,
                                              DeleteProductChargeOpenAccountUseCase deleteUseCase,
                                              ReactivateProductChargeOpenAccountUseCase reactivateUseCase,
                                              VoidProductChargeOpenAccountUseCase voidUseCase,
                                              Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByOpenAccountUseCase = listByOpenAccountUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.voidUseCase = voidUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductChargeOpenAccountResponse create(
            @Valid @RequestBody CreateProductChargeOpenAccountRequest request) {
        return toResponse(createUseCase.execute(
            new CreateProductChargeOpenAccountCommand(
                request.animalId(), request.productId(), request.openAccountId(),
                authz.currentCompanyId(), authz.currentEmployeeId())));
    }

    @GetMapping
    public List<ProductChargeOpenAccountResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-open-account/{openAccountId}")
    public List<ProductChargeOpenAccountResponse> listByOpenAccount(@PathVariable Long openAccountId) {
        return listByOpenAccountUseCase.listByOpenAccount(openAccountId, authz.currentCompanyId())
            .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ProductChargeOpenAccountResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public ProductChargeOpenAccountResponse update(
            @PathVariable Long id, @Valid @RequestBody UpdateProductChargeOpenAccountRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateProductChargeOpenAccountCommand(
                id, request.animalId(), request.productId(), request.openAccountId(),
                authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    @PatchMapping("/{id}/enable")
    public ProductChargeOpenAccountResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyId()));
    }

    @PatchMapping("/{id}/void")
    public ProductChargeOpenAccountResponse voidCharge(
            @PathVariable Long id, @Valid @RequestBody VoidProductChargeOpenAccountRequest request) {
        return toResponse(voidUseCase.execute(
            new VoidProductChargeOpenAccountCommand(
                id, authz.currentCompanyId(), authz.currentEmployeeId(), request.reason())));
    }

    private ProductChargeOpenAccountResponse toResponse(ProductChargeOpenAccountDto dto) {
        AnimalSummaryDto a = dto.animal();
        ProductSummaryDto p = dto.product();
        OpenAccountSummaryDto o = dto.openAccount();
        EmployeeSummaryDto e = dto.createdBy();
        EmployeeSummaryDto v = dto.voidedBy();
        return new ProductChargeOpenAccountResponse(
            dto.id(),
            new AnimalSummary(a.id(), a.name(), a.code()),
            new ProductSummary(p.id(), p.name(), p.code(), p.salePrice()),
            dto.unitPrice(),
            new OpenAccountSummary(o.id(), o.companyId()),
            e == null ? null : new EmployeeSummary(e.id(), e.name()),
            dto.createdDate(),
            dto.enabled(),
            dto.voided(),
            v == null ? null : new EmployeeSummary(v.id(), v.name()),
            dto.voidedAt(),
            dto.voidReason());
    }
}
