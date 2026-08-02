package com.vetsoftware.app.laboratorytesttype.application.port.out;

import com.vetsoftware.app.laboratorytesttype.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
