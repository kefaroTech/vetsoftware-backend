package com.vetsoftware.app.animal.application.port.out;

import com.vetsoftware.app.animal.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
