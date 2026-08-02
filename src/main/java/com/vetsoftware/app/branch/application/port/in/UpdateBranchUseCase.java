package com.vetsoftware.app.branch.application.port.in;

import com.vetsoftware.app.branch.application.command.UpdateBranchCommand;
import com.vetsoftware.app.branch.application.dto.BranchDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateBranchUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('branch.update') and @authz.isMyCompany(#command.companyId))")
    BranchDto execute(UpdateBranchCommand command);
}
