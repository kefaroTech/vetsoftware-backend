package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import com.vetsoftware.app.servicechargeopenaccount.application.port.in.DeleteServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service_charge_open_account.delete")
@Service
public class DeleteServiceChargeOpenAccountService implements DeleteServiceChargeOpenAccountUseCase {
    private final ServiceChargeOpenAccountRepository repository;
    private final OpenAccountRefresher refresher;

    public DeleteServiceChargeOpenAccountService(ServiceChargeOpenAccountRepository repository,
                                                 OpenAccountRefresher refresher) {
        this.repository = repository;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        ServiceChargeOpenAccount charge = repository.findById(id)
            .orElseThrow(() -> new ServiceChargeOpenAccountNotFoundException(id));
        Long openAccountId = charge.getOpenAccount().id();
        repository.delete(id);
        refresher.refresh(openAccountId);
    }
}
