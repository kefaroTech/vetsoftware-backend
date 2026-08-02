package com.vetsoftware.app.goodsreceipt.application.port.out;

import com.vetsoftware.app.goodsreceipt.domain.CompanyRef;
import java.util.Optional;

public interface CompanyQueryPort {
  Optional<CompanyRef> findById(Long companyId);
}
