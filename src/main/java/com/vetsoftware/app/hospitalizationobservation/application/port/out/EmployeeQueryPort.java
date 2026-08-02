package com.vetsoftware.app.hospitalizationobservation.application.port.out;

import com.vetsoftware.app.hospitalizationobservation.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
  Optional<EmployeeRef> findById(Long employeeId);
}
