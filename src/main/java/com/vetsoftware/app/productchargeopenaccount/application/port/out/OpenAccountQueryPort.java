package com.vetsoftware.app.productchargeopenaccount.application.port.out;

import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import java.util.Optional;

public interface OpenAccountQueryPort {
    Optional<OpenAccountRef> findById(Long openAccountId);
}
