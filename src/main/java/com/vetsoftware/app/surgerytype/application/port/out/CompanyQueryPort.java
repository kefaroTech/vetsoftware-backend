package com.vetsoftware.app.surgerytype.application.port.out;

import com.vetsoftware.app.surgerytype.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
