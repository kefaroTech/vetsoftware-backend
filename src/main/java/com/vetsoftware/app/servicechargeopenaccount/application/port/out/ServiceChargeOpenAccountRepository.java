package com.vetsoftware.app.servicechargeopenaccount.application.port.out;

import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import java.util.List;
import java.util.Optional;

public interface ServiceChargeOpenAccountRepository {
    ServiceChargeOpenAccount save(ServiceChargeOpenAccount charge);
    Optional<ServiceChargeOpenAccount> findById(Long id);
    List<ServiceChargeOpenAccount> findAll();
    List<ServiceChargeOpenAccount> findByOpenAccountId(Long openAccountId);
    void delete(Long id);
    int reactivate(Long id);
}
