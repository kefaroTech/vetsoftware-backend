package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.FindServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "service_charge_open_account.find")
@Service
public class FindServiceChargeOpenAccountService implements FindServiceChargeOpenAccountUseCase {
    private final ServiceChargeOpenAccountRepository repository;

    public FindServiceChargeOpenAccountService(ServiceChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public ServiceChargeOpenAccountDto findById(Long id) {
        return ServiceChargeOpenAccountDto.from(repository.findById(id)
            .orElseThrow(() -> new ServiceChargeOpenAccountNotFoundException(id)));
    }
}
