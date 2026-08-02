package com.vetsoftware.app.owner.application.port.out;

import com.vetsoftware.app.owner.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
