package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.command.SearchEmployeesCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SearchEmployeesUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('employee.read') and"
            + " @authz.isMyCompany(#command.companyId))")
    PageResult<EmployeeDto> search(SearchEmployeesCommand command);
}
