package com.vetsoftware.app.generalchargeopenaccount.application.port.out;

import com.vetsoftware.app.generalchargeopenaccount.domain.OpenAccountRef;
import java.util.Optional;

public interface OpenAccountQueryPort {
    Optional<OpenAccountRef> findById(Long openAccountId);
}
