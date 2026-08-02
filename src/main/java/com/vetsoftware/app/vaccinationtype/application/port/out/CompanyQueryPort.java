package com.vetsoftware.app.vaccinationtype.application.port.out;

import com.vetsoftware.app.vaccinationtype.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
