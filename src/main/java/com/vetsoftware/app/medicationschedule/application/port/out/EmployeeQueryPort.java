package com.vetsoftware.app.medicationschedule.application.port.out;

import com.vetsoftware.app.medicationschedule.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
    Optional<EmployeeRef> findById(Long employeeId);
}
