package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.FindDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "debt.open.account.find")
@Service
public class FindDebtOpenAccountService implements FindDebtOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;

    public FindDebtOpenAccountService(DebtOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public DebtOpenAccountDto findById(Long id, Long companyId) {
        return DebtOpenAccountDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new DebtOpenAccountNotFoundException(id)));
    }
}
