package com.vetsoftware.app.daycare.application.port.out;

import com.vetsoftware.app.daycare.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
