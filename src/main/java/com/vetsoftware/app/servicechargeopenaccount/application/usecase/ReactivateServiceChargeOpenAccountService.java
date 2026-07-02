package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.ReactivateServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service_charge_open_account.reactivate")
@Service
public class ReactivateServiceChargeOpenAccountService implements ReactivateServiceChargeOpenAccountUseCase {
    private final ServiceChargeOpenAccountRepository repository;
    private final OpenAccountRefresher refresher;

    public ReactivateServiceChargeOpenAccountService(ServiceChargeOpenAccountRepository repository,
                                                     OpenAccountRefresher refresher) {
        this.repository = repository;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public ServiceChargeOpenAccountDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0) throw new ServiceChargeOpenAccountNotFoundException(id);
        ServiceChargeOpenAccount charge = repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ServiceChargeOpenAccountNotFoundException(id));
        refresher.refresh(companyId, charge.getOpenAccount().id());
        return ServiceChargeOpenAccountDto.from(charge);
    }
}
