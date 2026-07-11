package com.vetsoftware.app.branch.application.port.in;

import com.vetsoftware.app.branch.application.dto.BranchDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface DeactivateBranchUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('branch.update') and @authz.isMyCompany(#companyId))")
    BranchDto execute(Long id, Long companyId);
}
