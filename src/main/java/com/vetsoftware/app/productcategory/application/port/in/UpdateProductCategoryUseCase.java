package com.vetsoftware.app.productcategory.application.port.in;

import com.vetsoftware.app.productcategory.application.command.UpdateProductCategoryCommand;
import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateProductCategoryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('productCategory.update') and @authz.isMyCompany(#command.companyId))")
    ProductCategoryDto execute(UpdateProductCategoryCommand command);
}
