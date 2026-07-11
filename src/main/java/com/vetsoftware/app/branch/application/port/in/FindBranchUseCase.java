package com.vetsoftware.app.branch.application.port.in;

import com.vetsoftware.app.branch.application.dto.BranchDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindBranchUseCase {
    // Lectura de una sede de la propia empresa: sin permiso específico (ver ListBranchesUseCase).
    @PreAuthorize("hasAuthority('admin.all') or @authz.isMyCompany(#companyId)")
    BranchDto findById(Long id, Long companyId);
}
