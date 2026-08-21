package com.vetsoftware.app.generalchargeopenaccount.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import java.util.List;
import java.util.Optional;

public interface GeneralChargeOpenAccountRepository {
    GeneralChargeOpenAccount save(GeneralChargeOpenAccount charge);

    Optional<GeneralChargeOpenAccount> findById(Long id);

    /**
     * Lectura scoped a la empresa (vía la cuenta): evita IDOR cross-tenant al
     * consultar un cargo por id directo.
     */
    Optional<GeneralChargeOpenAccount> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Cargo ya registrado con esta idempotency key en la cuenta (para deduplicar
     * reintentos).
     */
    Optional<GeneralChargeOpenAccount> findByOpenAccountIdAndClientRequestId(Long openAccountId,
            String clientRequestId);

    List<GeneralChargeOpenAccount> findAll();

    PageResult<GeneralChargeOpenAccount> findAllByCompanyId(Long companyId, int page, int pageSize);

    List<GeneralChargeOpenAccount> findByOpenAccountIdAndCompanyId(Long openAccountId,
            Long companyId);

    void delete(Long id);
}
