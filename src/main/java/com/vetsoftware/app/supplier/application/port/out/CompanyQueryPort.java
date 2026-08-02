package com.vetsoftware.app.supplier.application.port.out;

import com.vetsoftware.app.supplier.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
