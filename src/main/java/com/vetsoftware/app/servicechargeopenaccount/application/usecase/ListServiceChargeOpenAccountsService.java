package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.PageResult;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.ListServiceChargeOpenAccountsUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "service.charge.open.account.list.all")
@Service
public class ListServiceChargeOpenAccountsService implements ListServiceChargeOpenAccountsUseCase {
    private final ServiceChargeOpenAccountRepository repository;

    public ListServiceChargeOpenAccountsService(ServiceChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<ServiceChargeOpenAccountDto> listAll(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(ServiceChargeOpenAccountDto::from);
    }
}
