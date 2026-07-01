package com.vetsoftware.app.productcategory.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.productcategory.application.command.CreateProductCategoryCommand;
import com.vetsoftware.app.productcategory.application.command.UpdateProductCategoryCommand;
import com.vetsoftware.app.productcategory.application.dto.CompanySummaryDto;
import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import com.vetsoftware.app.productcategory.application.port.in.CreateProductCategoryUseCase;
import com.vetsoftware.app.productcategory.application.port.in.DeleteProductCategoryUseCase;
import com.vetsoftware.app.productcategory.application.port.in.FindProductCategoryUseCase;
import com.vetsoftware.app.productcategory.application.port.in.ListProductCategoriesUseCase;
import com.vetsoftware.app.productcategory.application.port.in.ReactivateProductCategoryUseCase;
import com.vetsoftware.app.productcategory.application.port.in.UpdateProductCategoryUseCase;
import com.vetsoftware.app.productcategory.infrastructure.web.request.CreateProductCategoryRequest;
import com.vetsoftware.app.productcategory.infrastructure.web.request.UpdateProductCategoryRequest;
import com.vetsoftware.app.productcategory.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.productcategory.infrastructure.web.response.ProductCategoryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product-categories")
public class ProductCategoryController {
    private final CreateProductCategoryUseCase createUseCase;
    private final UpdateProductCategoryUseCase updateUseCase;
    private final FindProductCategoryUseCase findUseCase;
    private final ListProductCategoriesUseCase listUseCase;
    private final DeleteProductCategoryUseCase deleteUseCase;
    private final ReactivateProductCategoryUseCase reactivateUseCase;
    private final Authz authz;

    public ProductCategoryController(CreateProductCategoryUseCase createUseCase,
                                     UpdateProductCategoryUseCase updateUseCase,
                                     FindProductCategoryUseCase findUseCase,
                                     ListProductCategoriesUseCase listUseCase,
                                     DeleteProductCategoryUseCase deleteUseCase,
                                     ReactivateProductCategoryUseCase reactivateUseCase,
                                     Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductCategoryResponse create(@Valid @RequestBody CreateProductCategoryRequest request) {
        return toResponse(createUseCase.execute(
                new CreateProductCategoryCommand(request.name(), request.description(),
                        authz.currentCompanyId())));
    }

    @GetMapping
    public List<ProductCategoryResponse> list() {
        return listUseCase.listByCompany(authz.currentCompanyId())
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ProductCategoryResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public ProductCategoryResponse update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateProductCategoryRequest request) {
        return toResponse(updateUseCase.execute(
                new UpdateProductCategoryCommand(id, request.name(), request.description(),
                        authz.currentCompanyId(), authz.currentEmployeeIdOrNull(), request.version())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    @PatchMapping("/{id}/enable")
    public ProductCategoryResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyId()));
    }

    private ProductCategoryResponse toResponse(ProductCategoryDto dto) {
        CompanySummaryDto c = dto.company();
        return new ProductCategoryResponse(
                dto.id(), dto.name(), dto.description(),
                c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.createdDate(),
                dto.updatedDate(),
                dto.updatedBy(),
                dto.version(),
                dto.enabled());
    }
}
