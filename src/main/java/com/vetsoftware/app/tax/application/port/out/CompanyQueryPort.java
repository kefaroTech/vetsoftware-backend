package com.vetsoftware.app.tax.application.port.out;

import com.vetsoftware.app.tax.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
