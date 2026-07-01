package com.vetsoftware.app.productcategory.application.usecase;

import com.vetsoftware.app.productcategory.application.command.UpdateProductCategoryCommand;
import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import com.vetsoftware.app.productcategory.application.port.in.UpdateProductCategoryUseCase;
import com.vetsoftware.app.productcategory.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.domain.CompanyRef;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNameAlreadyExistsException;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product_category.update")
@Service
public class UpdateProductCategoryService implements UpdateProductCategoryUseCase {
    private final ProductCategoryRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public UpdateProductCategoryService(ProductCategoryRepository repository,
                                        CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public ProductCategoryDto execute(UpdateProductCategoryCommand command) {
        ProductCategory productCategory = repository.findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new ProductCategoryNotFoundException(command.id()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        if (repository.existsByCompanyIdAndNameExcludingId(command.companyId(), command.name(), command.id())) {
            throw new ProductCategoryNameAlreadyExistsException(command.name());
        }
        productCategory.update(command.name(), command.description(), company, command.updatedBy(), command.version());
        return ProductCategoryDto.from(repository.save(productCategory));
    }
}
