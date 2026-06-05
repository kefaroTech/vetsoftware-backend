package com.vetsoftware.app.openaccount.application.port.out;

import com.vetsoftware.app.openaccount.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
