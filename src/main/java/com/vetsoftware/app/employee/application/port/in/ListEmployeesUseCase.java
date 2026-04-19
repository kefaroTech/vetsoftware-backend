package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import java.util.List;

public interface ListEmployeesUseCase {
    List<EmployeeDto> listAll();
}
