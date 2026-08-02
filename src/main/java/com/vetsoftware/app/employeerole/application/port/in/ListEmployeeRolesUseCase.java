package com.vetsoftware.app.employeerole.application.port.in;

import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListEmployeeRolesUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    List<EmployeeRoleDto> listAll();
}
