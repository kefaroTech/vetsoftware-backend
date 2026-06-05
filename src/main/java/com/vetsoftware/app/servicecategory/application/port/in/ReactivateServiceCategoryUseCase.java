package com.vetsoftware.app.servicecategory.application.port.in;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateServiceCategoryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('serviceCategory.delete')")
    ServiceCategoryDto execute(Long id);
}
