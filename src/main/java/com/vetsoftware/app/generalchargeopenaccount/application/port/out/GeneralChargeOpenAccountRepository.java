package com.vetsoftware.app.generalchargeopenaccount.application.port.out;

import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import java.util.List;
import java.util.Optional;

public interface GeneralChargeOpenAccountRepository {
    GeneralChargeOpenAccount save(GeneralChargeOpenAccount charge);
    Optional<GeneralChargeOpenAccount> findById(Long id);
    List<GeneralChargeOpenAccount> findAll();
    List<GeneralChargeOpenAccount> findByOpenAccountId(Long openAccountId);
    void delete(Long id);
    int reactivate(Long id);
}
