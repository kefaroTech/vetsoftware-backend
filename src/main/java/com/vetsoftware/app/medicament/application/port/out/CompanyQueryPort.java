package com.vetsoftware.app.medicament.application.port.out;

import com.vetsoftware.app.medicament.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
