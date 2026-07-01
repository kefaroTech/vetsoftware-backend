package com.vetsoftware.app.product.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.product.application.command.CreateProductCommand;
import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.command.UpdateProductCommand;
import com.vetsoftware.app.product.application.dto.CompanySummaryDto;
import com.vetsoftware.app.product.application.dto.PageResult;
import com.vetsoftware.app.product.application.dto.ProductCategorySummaryDto;
import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.dto.TaxSummaryDto;
import com.vetsoftware.app.product.application.port.in.CreateProductUseCase;
import com.vetsoftware.app.product.application.port.in.DeleteProductUseCase;
import com.vetsoftware.app.product.application.port.in.FindProductUseCase;
import com.vetsoftware.app.product.application.port.in.ListProductsUseCase;
import com.vetsoftware.app.product.application.port.in.ReactivateProductUseCase;
import com.vetsoftware.app.product.application.port.in.SearchProductsUseCase;
import com.vetsoftware.app.product.application.port.in.UpdateProductUseCase;
import com.vetsoftware.app.product.infrastructure.web.request.CreateProductRequest;
import com.vetsoftware.app.product.infrastructure.web.request.UpdateProductRequest;
import com.vetsoftware.app.product.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.product.infrastructure.web.response.ProductCategorySummary;
import com.vetsoftware.app.product.infrastructure.web.response.ProductResponse;
import com.vetsoftware.app.product.infrastructure.web.response.TaxSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final CreateProductUseCase createUseCase;
    private final UpdateProductUseCase updateUseCase;
    private final FindProductUseCase findUseCase;
    private final ListProductsUseCase listUseCase;
    private final SearchProductsUseCase searchUseCase;
    private final DeleteProductUseCase deleteUseCase;
    private final ReactivateProductUseCase reactivateUseCase;
    private final Authz authz;

    public ProductController(CreateProductUseCase createUseCase,
                             UpdateProductUseCase updateUseCase,
                             FindProductUseCase findUseCase,
                             ListProductsUseCase listUseCase,
                             SearchProductsUseCase searchUseCase,
                             DeleteProductUseCase deleteUseCase,
                             ReactivateProductUseCase reactivateUseCase,
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
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return toResponse(createUseCase.execute(
            new CreateProductCommand(
                request.name(), request.code(), request.purchasePrice(), request.salePrice(),
                request.currentStock(), request.minStock(), request.provider(),
                request.expireDate(), request.lotNumber(), request.notes(), request.taxTreatment(),
                request.productCategoryId(), request.taxId(),
                authz.currentCompanyId())));
    }

    @GetMapping
    public List<ProductResponse> listByCompany() {
        return listUseCase.listByCompany(authz.currentCompanyId())
            .stream().map(this::toResponse).toList();
    }

    @GetMapping("/search")
    public PageResponse<ProductResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long productCategoryId,
            @RequestParam(required = false) Long taxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<ProductDto> result = searchUseCase.execute(new SearchProductsCommand(
            authz.currentCompanyId(), name, code, productCategoryId, taxId, page, pageSize));
        return new PageResponse<>(
            result.content().stream().map(this::toResponse).toList(),
            result.page(), result.pageSize(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateProductCommand(
                id, request.name(), request.code(), request.purchasePrice(), request.salePrice(),
                request.currentStock(), request.minStock(), request.provider(),
                request.expireDate(), request.lotNumber(), request.notes(), request.taxTreatment(),
                request.productCategoryId(), request.taxId(),
                authz.currentCompanyId(), authz.currentEmployeeIdOrNull(), request.version())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    @PatchMapping("/{id}/enable")
    public ProductResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyId()));
    }

    private ProductResponse toResponse(ProductDto dto) {
        ProductCategorySummaryDto pc = dto.productCategory();
        TaxSummaryDto t = dto.tax();
        CompanySummaryDto c = dto.company();
        return new ProductResponse(
            dto.id(), dto.name(), dto.code(), dto.purchasePrice(), dto.salePrice(),
            dto.currentStock(), dto.minStock(), dto.provider(), dto.taxTreatment(), dto.expireDate(),
            dto.lotNumber(), dto.notes(),
            new ProductCategorySummary(pc.id(), pc.name()),
            t == null ? null : new TaxSummary(t.id(), t.name(), t.percentage()),
            new CompanySummary(c.id(), c.name(), c.identifier()),
            dto.createdDate(), dto.updatedDate(), dto.updatedBy(), dto.version(), dto.enabled());
    }
}
