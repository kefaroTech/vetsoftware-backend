package com.vetsoftware.app.consultation.application.port.out;

import com.vetsoftware.app.consultation.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
