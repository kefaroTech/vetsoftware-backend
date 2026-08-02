package com.vetsoftware.app.product.application.port.out;

import com.vetsoftware.app.product.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
