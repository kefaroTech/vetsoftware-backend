package com.vetsoftware.app.purchaseorder.application.port.out;

import com.vetsoftware.app.purchaseorder.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
