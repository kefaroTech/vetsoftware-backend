package com.vetsoftware.app.employeerole.application.port.in;

import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateEmployeeRoleUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('employeerole.update')"
            + " and @authz.isMyCompany(#companyId))")
    EmployeeRoleDto execute(Long id, Long companyId);
}
