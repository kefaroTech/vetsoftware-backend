package com.vetsoftware.app.permission.application.port.out;

import com.vetsoftware.app.permission.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
