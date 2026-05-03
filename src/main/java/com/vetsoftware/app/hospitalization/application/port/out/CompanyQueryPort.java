package com.vetsoftware.app.hospitalization.application.port.out;

import com.vetsoftware.app.hospitalization.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
