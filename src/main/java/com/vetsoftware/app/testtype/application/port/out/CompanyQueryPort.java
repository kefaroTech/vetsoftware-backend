package com.vetsoftware.app.testtype.application.port.out;

import com.vetsoftware.app.testtype.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
