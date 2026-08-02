package com.vetsoftware.app.servicecategory.application.port.out;

import com.vetsoftware.app.servicecategory.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
