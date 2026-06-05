package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.ListServiceChargeOpenAccountsUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "service_charge_open_account.list_all")
@Service
public class ListServiceChargeOpenAccountsService implements ListServiceChargeOpenAccountsUseCase {
    private final ServiceChargeOpenAccountRepository repository;

    public ListServiceChargeOpenAccountsService(ServiceChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ServiceChargeOpenAccountDto> listAll() {
        return repository.findAll().stream().map(ServiceChargeOpenAccountDto::from).toList();
    }
}
