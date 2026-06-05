package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.FindDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "debt_open_account.find")
@Service
public class FindDebtOpenAccountService implements FindDebtOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;

    public FindDebtOpenAccountService(DebtOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public DebtOpenAccountDto findById(Long id) {
        return DebtOpenAccountDto.from(repository.findById(id)
            .orElseThrow(() -> new DebtOpenAccountNotFoundException(id)));
    }
}
