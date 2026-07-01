package com.vetsoftware.app.servicecategory.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteServiceCategoryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('serviceCategory.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
