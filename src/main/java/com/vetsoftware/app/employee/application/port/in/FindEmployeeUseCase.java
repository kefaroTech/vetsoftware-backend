package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;

public interface FindEmployeeUseCase {
    EmployeeDto findById(Long id);
}
