package com.vetsoftware.app.productcategory.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteProductCategoryUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('productCategory.delete') and @authz.isMyCompany(#companyId))")
  void execute(Long id, Long companyId);
}
