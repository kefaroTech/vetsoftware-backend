package com.vetsoftware.app.productcategory.application.usecase;

import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import com.vetsoftware.app.productcategory.application.port.in.ReactivateProductCategoryUseCase;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product_category.reactivate")
@Service
public class ReactivateProductCategoryService implements ReactivateProductCategoryUseCase {
    private final ProductCategoryRepository repository;

    public ReactivateProductCategoryService(ProductCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ProductCategoryDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0) throw new ProductCategoryNotFoundException(id);
        return ProductCategoryDto.from(repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ProductCategoryNotFoundException(id)));
    }
}
