package com.vetsoftware.app.hospitalizationprogressnote.application.port.out;

import com.vetsoftware.app.hospitalizationprogressnote.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
  Optional<EmployeeRef> findById(Long employeeId);
}
