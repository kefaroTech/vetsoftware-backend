package com.vetsoftware.app.hospitalizationmedication.application.port.out;

import com.vetsoftware.app.hospitalizationmedication.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
    Optional<EmployeeRef> findById(Long employeeId);
}
