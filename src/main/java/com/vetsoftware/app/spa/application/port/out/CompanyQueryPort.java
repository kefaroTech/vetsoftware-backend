package com.vetsoftware.app.spa.application.port.out;

import com.vetsoftware.app.spa.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
