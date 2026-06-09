package com.vetsoftware.app.service.application.port.out;

import com.vetsoftware.app.service.domain.TaxRef;
import java.util.Optional;

public interface TaxQueryPort {
    Optional<TaxRef> findById(Long taxId, Long companyId);
}
