package com.vetsoftware.app.service.application.port.out;

import com.vetsoftware.app.service.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
