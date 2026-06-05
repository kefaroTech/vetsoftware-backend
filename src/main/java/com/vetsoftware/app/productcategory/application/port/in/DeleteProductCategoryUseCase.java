package com.vetsoftware.app.productcategory.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteProductCategoryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('productCategory.delete')")
    void execute(Long id);
}
