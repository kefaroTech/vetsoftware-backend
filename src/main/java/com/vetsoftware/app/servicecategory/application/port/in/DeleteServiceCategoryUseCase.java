package com.vetsoftware.app.servicecategory.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteServiceCategoryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('serviceCategory.delete')")
    void execute(Long id);
}
