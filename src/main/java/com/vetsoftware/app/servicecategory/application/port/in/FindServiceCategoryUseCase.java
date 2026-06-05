package com.vetsoftware.app.servicecategory.application.port.in;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindServiceCategoryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('serviceCategory.read')")
    ServiceCategoryDto findById(Long id);
}
