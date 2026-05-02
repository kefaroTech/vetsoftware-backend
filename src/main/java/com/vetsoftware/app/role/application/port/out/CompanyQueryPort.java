package com.vetsoftware.app.role.application.port.out;

import com.vetsoftware.app.role.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
