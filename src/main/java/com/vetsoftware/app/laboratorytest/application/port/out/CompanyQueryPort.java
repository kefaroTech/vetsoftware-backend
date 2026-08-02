package com.vetsoftware.app.laboratorytest.application.port.out;

import com.vetsoftware.app.laboratorytest.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
