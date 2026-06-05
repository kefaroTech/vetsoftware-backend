package com.vetsoftware.app.servicecategory.application.port.in;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListServiceCategoriesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('serviceCategory.read') and @authz.isMyCompany(#companyId))")
    List<ServiceCategoryDto> listByCompany(Long companyId);
}
