package com.vetsoftware.app.productcategory.application.port.in;

import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindProductCategoryUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('productCategory.read') and @authz.isMyCompany(#companyId))")
  ProductCategoryDto findById(Long id, Long companyId);
}
