package com.vetsoftware.app.product.infrastructure.persistence;

import com.vetsoftware.app.product.application.port.out.ProductCategoryQueryPort;
import com.vetsoftware.app.product.domain.ProductCategoryRef;
import com.vetsoftware.app.productcategory.infrastructure.persistence.ProductCategoryJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("productJpaProductCategoryQueryPort")
public class JpaProductCategoryQueryPort implements ProductCategoryQueryPort {
    private final ProductCategoryJpaRepository productCategoryJpaRepository;

    public JpaProductCategoryQueryPort(ProductCategoryJpaRepository productCategoryJpaRepository) {
        this.productCategoryJpaRepository = productCategoryJpaRepository;
    }

    @Override
    public Optional<ProductCategoryRef> findById(Long productCategoryId, Long companyId) {
        return productCategoryJpaRepository.findByIdAndCompany_Id(productCategoryId, companyId)
                .map(e -> new ProductCategoryRef(e.getId(), e.getName()));
    }
}
