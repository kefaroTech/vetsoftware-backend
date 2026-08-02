package com.vetsoftware.app.dianprovider.application.port.out;

import com.vetsoftware.app.dianprovider.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
