package com.vetsoftware.app.hospitalizationprocedure.application.port.out;

import com.vetsoftware.app.hospitalizationprocedure.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
    Optional<EmployeeRef> findById(Long employeeId);
}
