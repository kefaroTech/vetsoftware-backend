package com.vetsoftware.app.application.port.in;

import com.vetsoftware.app.application.dto.EmployeeDto;

public interface FindEmployeeUseCase {
    EmployeeDto findById(Long id);
}
