package com.vetsoftware.app.productcategory.application.usecase;

import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import com.vetsoftware.app.productcategory.application.port.in.FindProductCategoryUseCase;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "product_category.find")
@Service
public class FindProductCategoryService implements FindProductCategoryUseCase {
    private final ProductCategoryRepository repository;

    public FindProductCategoryService(ProductCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProductCategoryDto findById(Long id, Long companyId) {
        return ProductCategoryDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ProductCategoryNotFoundException(id)));
    }
}
