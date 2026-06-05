package com.vetsoftware.app.productcategory.application.port.in;

import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateProductCategoryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('productCategory.delete')")
    ProductCategoryDto execute(Long id);
}
