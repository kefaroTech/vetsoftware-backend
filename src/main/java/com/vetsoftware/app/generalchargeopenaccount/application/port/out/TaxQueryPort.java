package com.vetsoftware.app.generalchargeopenaccount.application.port.out;

import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
import java.util.Optional;

public interface TaxQueryPort {
    Optional<TaxRef> findById(Long taxId);
}
