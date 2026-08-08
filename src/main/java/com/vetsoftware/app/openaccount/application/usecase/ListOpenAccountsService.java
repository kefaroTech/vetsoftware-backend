package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.application.port.in.ListOpenAccountsUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "open.account.list.by.company")
@Service
public class ListOpenAccountsService implements ListOpenAccountsUseCase {
    private final OpenAccountRepository repository;

    public ListOpenAccountsService(OpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<OpenAccountDto> listByCompany(Long companyId, Long branchId, int page,
            int pageSize) {
        return repository.findAllByCompanyId(companyId, branchId, page, pageSize)
                .map(OpenAccountDto::from);
    }
}
