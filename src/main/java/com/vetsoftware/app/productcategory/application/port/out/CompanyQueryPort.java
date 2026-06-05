package com.vetsoftware.app.productcategory.application.port.out;

import com.vetsoftware.app.productcategory.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
