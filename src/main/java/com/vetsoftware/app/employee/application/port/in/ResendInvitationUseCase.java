package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.command.ResendInvitationCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Reenvía la invitación a un empleado en estado INVITED, con una nueva contraseña provisional. */
public interface ResendInvitationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('employee.create') and @authz.isMyCompany(#command.companyId))")
    EmployeeDto execute(ResendInvitationCommand command);
}
