package com.vetsoftware.app.surgery.application.port.out;

import com.vetsoftware.app.surgery.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
