package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import com.vetsoftware.app.generalchargeopenaccount.application.port.in.DeleteGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "general_charge_open_account.delete")
@Service
public class DeleteGeneralChargeOpenAccountService implements DeleteGeneralChargeOpenAccountUseCase {
    private final GeneralChargeOpenAccountRepository repository;
    private final OpenAccountRefresher refresher;

    public DeleteGeneralChargeOpenAccountService(GeneralChargeOpenAccountRepository repository,
                                                 OpenAccountRefresher refresher) {
        this.repository = repository;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        GeneralChargeOpenAccount charge = repository.findById(id)
            .orElseThrow(() -> new GeneralChargeOpenAccountNotFoundException(id));
        Long openAccountId = charge.getOpenAccount().id();
        repository.delete(id);
        refresher.refresh(openAccountId);
    }
}
