package com.vetsoftware.app.productcategory.application.port.out;

import com.vetsoftware.app.productcategory.domain.ProductCategory;
import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository {
    ProductCategory save(ProductCategory productCategory);
    Optional<ProductCategory> findById(Long id);
    List<ProductCategory> findAll();
    List<ProductCategory> findAllByCompanyId(Long companyId);
    void delete(Long id);
    int reactivate(Long id);
}
