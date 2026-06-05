package com.vetsoftware.app.product.application.port.out;

import com.vetsoftware.app.product.domain.TaxRef;
import java.util.Optional;

public interface TaxQueryPort {
    Optional<TaxRef> findById(Long taxId);
}
