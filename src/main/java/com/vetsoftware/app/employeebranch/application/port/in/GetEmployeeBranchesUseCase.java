package com.vetsoftware.app.employeebranch.application.port.in;

import com.vetsoftware.app.employeebranch.application.dto.EmployeeBranchesDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetEmployeeBranchesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('employee.read') and @authz.isMyCompany(#companyId))")
    EmployeeBranchesDto execute(Long employeeId, Long companyId);
}
