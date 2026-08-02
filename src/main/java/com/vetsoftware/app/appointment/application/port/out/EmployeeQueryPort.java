package com.vetsoftware.app.appointment.application.port.out;

import com.vetsoftware.app.appointment.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
  Optional<EmployeeRef> findByIdAndCompanyId(Long employeeId, Long companyId);
}
