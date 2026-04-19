package com.vetsoftware.app.application.port.in;

import com.vetsoftware.app.application.dto.EmployeeDto;
import java.util.List;

public interface ListEmployeesUseCase {
    List<EmployeeDto> listAll();
}
