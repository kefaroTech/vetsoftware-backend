package com.vetsoftware.app.productcategory.application.port.in;

import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateProductCategoryUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('productCategory.delete') and @authz.isMyCompany(#companyId))")
  ProductCategoryDto execute(Long id, Long companyId);
}
