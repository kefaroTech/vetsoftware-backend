package com.vetsoftware.app.servicecategory.application.port.in;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateServiceCategoryUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('serviceCategory.delete') and @authz.isMyCompany(#companyId))")
  ServiceCategoryDto execute(Long id, Long companyId);
}
