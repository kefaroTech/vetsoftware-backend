package com.vetsoftware.app.servicecategory.application.port.in;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindServiceCategoryUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('serviceCategory.read') and @authz.isMyCompany(#companyId))")
    ServiceCategoryDto findById(Long id, Long companyId);
}
