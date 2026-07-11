package com.vetsoftware.app.branch.application.port.in;

import com.vetsoftware.app.branch.application.command.CreateBranchCommand;
import com.vetsoftware.app.branch.application.dto.BranchDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateBranchUseCase {
    // hasRole('SYSTEM'): el auto-registro crea la sucursal "Principal" bajo contexto SYSTEM (SystemAuthRunner).
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM') or "
            + "(hasAuthority('branch.create') and @authz.isMyCompany(#command.companyId))")
    BranchDto execute(CreateBranchCommand command);
}
