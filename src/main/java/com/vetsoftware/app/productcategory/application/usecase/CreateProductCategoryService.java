package com.vetsoftware.app.productcategory.application.usecase;

import com.vetsoftware.app.productcategory.application.command.CreateProductCategoryCommand;
import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import com.vetsoftware.app.productcategory.application.port.in.CreateProductCategoryUseCase;
import com.vetsoftware.app.productcategory.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.domain.CompanyRef;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNameAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "product.category.create")
@Service
public class CreateProductCategoryService implements CreateProductCategoryUseCase {
    private final ProductCategoryRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public CreateProductCategoryService(ProductCategoryRepository repository,
                                        CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public ProductCategoryDto execute(CreateProductCategoryCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        if (repository.existsByCompanyIdAndName(command.companyId(), command.name())) {
            throw new ProductCategoryNameAlreadyExistsException(command.name());
        }
        return ProductCategoryDto.from(
                repository.save(ProductCategory.create(command.name(), command.description(), company)));
    }
}
