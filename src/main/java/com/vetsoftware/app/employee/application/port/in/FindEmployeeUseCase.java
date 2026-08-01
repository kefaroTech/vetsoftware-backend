package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindEmployeeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('employee.read') and @authz.isMyCompany(#companyId))")
    EmployeeDto findById(Long id, Long companyId);
}
