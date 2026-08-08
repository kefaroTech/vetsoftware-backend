package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.dto.PageResult;
import com.vetsoftware.app.debtopenaccount.application.port.in.ListDebtOpenAccountsUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "debt.open.account.list.all")
@Service
public class ListDebtOpenAccountsService implements ListDebtOpenAccountsUseCase {
    private final DebtOpenAccountRepository repository;

    public ListDebtOpenAccountsService(DebtOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<DebtOpenAccountDto> listAll(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(DebtOpenAccountDto::from);
    }
}
