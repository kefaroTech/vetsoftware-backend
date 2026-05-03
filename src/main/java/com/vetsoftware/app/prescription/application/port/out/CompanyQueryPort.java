package com.vetsoftware.app.prescription.application.port.out;

import com.vetsoftware.app.prescription.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
