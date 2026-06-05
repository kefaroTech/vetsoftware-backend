package com.vetsoftware.app.debtopenaccount.application.port.out;

import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import java.util.Optional;

public interface OpenAccountQueryPort {
    Optional<OpenAccountRef> findById(Long openAccountId);
}
