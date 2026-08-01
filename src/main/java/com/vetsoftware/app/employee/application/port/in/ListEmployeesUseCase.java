package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ListEmployeesUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    List<EmployeeDto> listAll();
}
