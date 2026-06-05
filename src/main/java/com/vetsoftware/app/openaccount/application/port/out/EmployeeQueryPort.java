package com.vetsoftware.app.openaccount.application.port.out;

import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
    Optional<EmployeeRef> findById(Long employeeId);
}
