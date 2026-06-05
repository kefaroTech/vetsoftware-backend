package com.vetsoftware.app.productchargeopenaccount.application.port.out;

import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import java.util.List;
import java.util.Optional;

public interface ProductChargeOpenAccountRepository {
    ProductChargeOpenAccount save(ProductChargeOpenAccount productChargeOpenAccount);
    Optional<ProductChargeOpenAccount> findById(Long id);
    List<ProductChargeOpenAccount> findAll();
    List<ProductChargeOpenAccount> findByOpenAccountId(Long openAccountId);
    void delete(Long id);
    int reactivate(Long id);
}
