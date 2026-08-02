package com.vetsoftware.app.animalalert.application.port.out;

import com.vetsoftware.app.animalalert.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
