package com.vetsoftware.app.problem.application.port.out;

import com.vetsoftware.app.problem.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
