package com.vetsoftware.app.animal.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteWeightRecordUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('animal.create') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long animalId, Long companyId);
}
