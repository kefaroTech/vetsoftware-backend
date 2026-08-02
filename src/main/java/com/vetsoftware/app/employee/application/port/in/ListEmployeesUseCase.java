package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListEmployeesUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  List<EmployeeDto> listAll();
}
