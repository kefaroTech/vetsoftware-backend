package com.vetsoftware.app.companytaxprofile.application.port.out;

import com.vetsoftware.app.companytaxprofile.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
