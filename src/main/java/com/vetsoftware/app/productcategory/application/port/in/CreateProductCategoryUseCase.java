package com.vetsoftware.app.productcategory.application.port.in;

import com.vetsoftware.app.productcategory.application.command.CreateProductCategoryCommand;
import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateProductCategoryUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('productCategory.create') and"
          + " @authz.isMyCompany(#command.companyId))")
  ProductCategoryDto execute(CreateProductCategoryCommand command);
}
