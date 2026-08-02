package com.vetsoftware.app.productcategory.infrastructure.persistence;

import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.productcategory.application.port.out.ProductChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaProductChildrenQueryPort implements ProductChildrenQueryPort {
  private final ProductJpaRepository productJpaRepository;

  public JpaProductChildrenQueryPort(ProductJpaRepository productJpaRepository) {
    this.productJpaRepository = productJpaRepository;
  }

  @Override
  public boolean existsActiveByProductCategoryId(Long categoryId) {
    return productJpaRepository.existsByProductCategory_Id(categoryId);
  }
}
