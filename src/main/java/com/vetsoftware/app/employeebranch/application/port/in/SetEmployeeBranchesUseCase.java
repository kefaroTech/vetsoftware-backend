package com.vetsoftware.app.employeebranch.application.port.in;

import com.vetsoftware.app.employeebranch.application.command.SetEmployeeBranchesCommand;
import com.vetsoftware.app.employeebranch.application.dto.EmployeeBranchesDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SetEmployeeBranchesUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('employee.update') and"
            + " @authz.isMyCompany(#command.companyId))")
    EmployeeBranchesDto execute(SetEmployeeBranchesCommand command);
}
