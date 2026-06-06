package com.vetsoftware.app.servicechargeopenaccount.application.port.out;

import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import java.math.BigDecimal;
import java.util.Optional;

public interface OpenAccountQueryPort {
    Optional<OpenAccountRef> findById(Long openAccountId);

    boolean isOpen(Long openAccountId);

    /** Saldo pendiente actual de la cuenta (total - abonos). ZERO si no existe. */
    BigDecimal outstandingAmount(Long openAccountId);
}
