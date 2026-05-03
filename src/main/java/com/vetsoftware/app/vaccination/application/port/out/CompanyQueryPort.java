package com.vetsoftware.app.vaccination.application.port.out;

import com.vetsoftware.app.vaccination.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
