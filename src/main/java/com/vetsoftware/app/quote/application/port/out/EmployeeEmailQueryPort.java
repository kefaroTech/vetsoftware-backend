package com.vetsoftware.app.quote.application.port.out;

import java.util.Optional;

public interface EmployeeEmailQueryPort {

    Optional<String> findEmail(Long employeeId, Long companyId);
}
