package com.vetsoftware.app.employee.application.port.out;

import com.vetsoftware.app.employee.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
