package com.vetsoftware.app.servicechargeopenaccount.application.port.out;

import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import java.util.List;
import java.util.Optional;

public interface ServiceChargeOpenAccountRepository {
    ServiceChargeOpenAccount save(ServiceChargeOpenAccount charge);
    Optional<ServiceChargeOpenAccount> findById(Long id);
    /** Lectura scoped a la empresa (vía la cuenta): evita IDOR cross-tenant al consultar un cargo por id directo. */
    Optional<ServiceChargeOpenAccount> findByIdAndCompanyId(Long id, Long companyId);
    /** Cargo ya registrado con esta idempotency key en la cuenta (para deduplicar reintentos). */
    Optional<ServiceChargeOpenAccount> findByOpenAccountIdAndClientRequestId(Long openAccountId, String clientRequestId);
    List<ServiceChargeOpenAccount> findAll();
    List<ServiceChargeOpenAccount> findAllByCompanyId(Long companyId);
    List<ServiceChargeOpenAccount> findByOpenAccountIdAndCompanyId(Long openAccountId, Long companyId);
    void delete(Long id);
    int reactivate(Long id, Long companyId);
}
