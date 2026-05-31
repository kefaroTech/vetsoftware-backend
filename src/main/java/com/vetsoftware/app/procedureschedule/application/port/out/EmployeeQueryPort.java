package com.vetsoftware.app.procedureschedule.application.port.out;

import com.vetsoftware.app.procedureschedule.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
    Optional<EmployeeRef> findById(Long employeeId);
}
