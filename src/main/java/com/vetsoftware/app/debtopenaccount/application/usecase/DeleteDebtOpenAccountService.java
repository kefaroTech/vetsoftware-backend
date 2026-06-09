package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.port.in.DeleteDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "debt_open_account.delete")
@Service
public class DeleteDebtOpenAccountService implements DeleteDebtOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;
    private final OpenAccountRefresher refresher;

    public DeleteDebtOpenAccountService(DebtOpenAccountRepository repository,
                                        OpenAccountRefresher refresher) {
        this.repository = repository;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        DebtOpenAccount debtOpenAccount = repository.findById(id)
            .orElseThrow(() -> new DebtOpenAccountNotFoundException(id));
        if (!debtOpenAccount.getOpenAccount().companyId().equals(companyId)) {
            throw new IllegalArgumentException("debt open account does not belong to company");
        }
        Long openAccountId = debtOpenAccount.getOpenAccount().id();
        repository.delete(id);
        refresher.refresh(openAccountId);
    }
}
