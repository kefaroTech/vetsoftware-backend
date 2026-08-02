package com.vetsoftware.app.deworming.application.port.out;

import com.vetsoftware.app.deworming.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
