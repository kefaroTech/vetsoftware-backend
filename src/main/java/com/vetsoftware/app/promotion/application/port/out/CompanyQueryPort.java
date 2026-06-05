package com.vetsoftware.app.promotion.application.port.out;

import com.vetsoftware.app.promotion.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
