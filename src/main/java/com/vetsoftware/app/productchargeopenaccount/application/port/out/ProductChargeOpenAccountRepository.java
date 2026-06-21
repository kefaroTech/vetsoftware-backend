package com.vetsoftware.app.productchargeopenaccount.application.port.out;

import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import java.util.List;
import java.util.Optional;

public interface ProductChargeOpenAccountRepository {
    ProductChargeOpenAccount save(ProductChargeOpenAccount productChargeOpenAccount);
    Optional<ProductChargeOpenAccount> findById(Long id);
    /** Lectura scoped a la empresa (vía la cuenta): evita IDOR cross-tenant al consultar un cargo por id directo. */
    Optional<ProductChargeOpenAccount> findByIdAndCompanyId(Long id, Long companyId);
    /** Cargo ya registrado con esta idempotency key en la cuenta (para deduplicar reintentos). */
    Optional<ProductChargeOpenAccount> findByOpenAccountIdAndClientRequestId(Long openAccountId, String clientRequestId);
    List<ProductChargeOpenAccount> findAll();
    List<ProductChargeOpenAccount> findByOpenAccountId(Long openAccountId);
    void delete(Long id);
    int reactivate(Long id);
}
