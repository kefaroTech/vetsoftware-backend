package com.vetsoftware.app.productchargeopenaccount.application.port.out;

import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import java.util.Optional;

public interface ProductQueryPort {
  Optional<ProductRef> findByIdAndCompanyId(Long productId, Long companyId);
}
