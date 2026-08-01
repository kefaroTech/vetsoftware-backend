package com.vetsoftware.app.servicecategory.application.port.in;

import com.vetsoftware.app.servicecategory.application.command.CreateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateServiceCategoryUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('serviceCategory.create') and @authz.isMyCompany(#command.companyId))")
    ServiceCategoryDto execute(CreateServiceCategoryCommand command);
}
