package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.ReactivateDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "debt_open_account.reactivate")
@Service
public class ReactivateDebtOpenAccountService implements ReactivateDebtOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;
    private final OpenAccountRefresher refresher;

    public ReactivateDebtOpenAccountService(DebtOpenAccountRepository repository,
                                            OpenAccountRefresher refresher) {
        this.repository = repository;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public DebtOpenAccountDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new DebtOpenAccountNotFoundException(id);
        DebtOpenAccount debtOpenAccount = repository.findById(id)
            .orElseThrow(() -> new DebtOpenAccountNotFoundException(id));
        if (!debtOpenAccount.getOpenAccount().companyId().equals(companyId)) {
            throw new IllegalArgumentException("debt open account does not belong to company");
        }
        refresher.refresh(debtOpenAccount.getOpenAccount().id());
        return DebtOpenAccountDto.from(debtOpenAccount);
    }
}
