package com.vetsoftware.app.product.application.port.out;

import com.vetsoftware.app.product.domain.ProductCategoryRef;
import java.util.Optional;

public interface ProductCategoryQueryPort {
  Optional<ProductCategoryRef> findById(Long productCategoryId, Long companyId);
}
