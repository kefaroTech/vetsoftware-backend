package com.vetsoftware.app.numberingresolution.application.port.out;

import com.vetsoftware.app.numberingresolution.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
