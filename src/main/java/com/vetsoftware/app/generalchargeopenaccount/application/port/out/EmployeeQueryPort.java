package com.vetsoftware.app.generalchargeopenaccount.application.port.out;

import com.vetsoftware.app.generalchargeopenaccount.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
    Optional<EmployeeRef> findById(Long employeeId);
}
