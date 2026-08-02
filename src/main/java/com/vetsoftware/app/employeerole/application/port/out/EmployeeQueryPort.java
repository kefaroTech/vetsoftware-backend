package com.vetsoftware.app.employeerole.application.port.out;

import com.vetsoftware.app.employeerole.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
  Optional<EmployeeRef> findById(Long employeeId);
}
