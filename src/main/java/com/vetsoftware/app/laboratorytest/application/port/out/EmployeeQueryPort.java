package com.vetsoftware.app.laboratorytest.application.port.out;

import com.vetsoftware.app.laboratorytest.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
    Optional<EmployeeRef> findById(Long employeeId);
}
