package com.vetsoftware.app.debtopenaccount.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import java.util.List;
import java.util.Optional;

public interface DebtOpenAccountRepository {
    DebtOpenAccount save(DebtOpenAccount debtOpenAccount);

    Optional<DebtOpenAccount> findById(Long id);

    /**
     * Lectura scoped a la empresa (vía la cuenta): evita IDOR cross-tenant al
     * consultar un abono por id directo.
     */
    Optional<DebtOpenAccount> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Abono ya registrado con esta idempotency key en la cuenta (para deduplicar
     * reintentos).
     */
    Optional<DebtOpenAccount> findByOpenAccountIdAndClientRequestId(Long openAccountId,
            String clientRequestId);

    List<DebtOpenAccount> findAll();

    PageResult<DebtOpenAccount> findAllByCompanyId(Long companyId, int page, int pageSize);

    List<DebtOpenAccount> findByOpenAccountIdAndCompanyId(Long openAccountId, Long companyId);

    void delete(Long id, Long companyId);

    int reactivate(Long id, Long companyId);
}
