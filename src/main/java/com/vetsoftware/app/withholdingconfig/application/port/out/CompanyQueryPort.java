package com.vetsoftware.app.withholdingconfig.application.port.out;

import com.vetsoftware.app.withholdingconfig.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
