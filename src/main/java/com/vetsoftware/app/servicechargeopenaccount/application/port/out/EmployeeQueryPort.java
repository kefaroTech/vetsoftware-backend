package com.vetsoftware.app.servicechargeopenaccount.application.port.out;

import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
  Optional<EmployeeRef> findByIdAndCompanyId(Long employeeId, Long companyId);
}
