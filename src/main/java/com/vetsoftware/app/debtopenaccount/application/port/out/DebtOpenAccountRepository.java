package com.vetsoftware.app.debtopenaccount.application.port.out;

import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import java.util.List;
import java.util.Optional;

public interface DebtOpenAccountRepository {
    DebtOpenAccount save(DebtOpenAccount debtOpenAccount);
    Optional<DebtOpenAccount> findById(Long id);
    /** Abono ya registrado con esta idempotency key en la cuenta (para deduplicar reintentos). */
    Optional<DebtOpenAccount> findByOpenAccountIdAndClientRequestId(Long openAccountId, String clientRequestId);
    List<DebtOpenAccount> findAll();
    List<DebtOpenAccount> findByOpenAccountId(Long openAccountId);
    void delete(Long id);
    int reactivate(Long id);
}
