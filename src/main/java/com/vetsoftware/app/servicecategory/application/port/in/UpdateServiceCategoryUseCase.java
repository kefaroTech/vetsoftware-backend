package com.vetsoftware.app.servicecategory.application.port.in;

import com.vetsoftware.app.servicecategory.application.command.UpdateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateServiceCategoryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('serviceCategory.update') and @authz.isMyCompany(#command.companyId))")
    ServiceCategoryDto execute(UpdateServiceCategoryCommand command);
}
