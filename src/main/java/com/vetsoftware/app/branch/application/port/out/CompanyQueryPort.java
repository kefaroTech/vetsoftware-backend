package com.vetsoftware.app.branch.application.port.out;

import com.vetsoftware.app.branch.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
