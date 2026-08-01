package com.vetsoftware.app.employeerole.application.port.in;

import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ListEmployeeRolesUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    List<EmployeeRoleDto> listAll();
}
