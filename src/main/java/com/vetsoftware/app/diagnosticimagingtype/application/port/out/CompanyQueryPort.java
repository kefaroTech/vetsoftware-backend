package com.vetsoftware.app.diagnosticimagingtype.application.port.out;

import com.vetsoftware.app.diagnosticimagingtype.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
