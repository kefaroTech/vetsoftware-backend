package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.command.InviteEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface InviteEmployeeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('employee.create') and @authz.isMyCompany(#command.companyId))")
    EmployeeDto execute(InviteEmployeeCommand command);
}
