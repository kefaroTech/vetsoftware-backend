package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.ListDebtOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "debt_open_account.list_by_open_account")
@Service
public class ListDebtOpenAccountsByOpenAccountService implements ListDebtOpenAccountsByOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;

    public ListDebtOpenAccountsByOpenAccountService(DebtOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<DebtOpenAccountDto> listByOpenAccount(Long openAccountId, Long companyId) {
        return repository.findByOpenAccountIdAndCompanyId(openAccountId, companyId).stream()
            .map(DebtOpenAccountDto::from).toList();
    }
}
