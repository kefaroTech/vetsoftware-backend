package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.ListServiceChargeOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "service.charge.open.account.list.by.open.account")
@Service
public class ListServiceChargeOpenAccountsByOpenAccountService
        implements ListServiceChargeOpenAccountsByOpenAccountUseCase {
    private final ServiceChargeOpenAccountRepository repository;

    public ListServiceChargeOpenAccountsByOpenAccountService(ServiceChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ServiceChargeOpenAccountDto> listByOpenAccount(Long openAccountId, Long companyId) {
        return repository.findByOpenAccountIdAndCompanyId(openAccountId, companyId).stream()
            .map(ServiceChargeOpenAccountDto::from).toList();
    }
}
