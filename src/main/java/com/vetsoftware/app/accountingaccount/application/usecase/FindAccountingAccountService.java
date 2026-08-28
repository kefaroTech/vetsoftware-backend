package com.vetsoftware.app.accountingaccount.application.usecase;

import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import com.vetsoftware.app.accountingaccount.application.port.in.FindAccountingAccountUseCase;
import com.vetsoftware.app.accountingaccount.application.port.out.AccountingAccountRepository;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/** Una cuenta por su id o por su codigo. */
@Observed(name = "accounting.account.find")
@Service
public class FindAccountingAccountService implements FindAccountingAccountUseCase {

    private final AccountingAccountRepository repository;

    public FindAccountingAccountService(AccountingAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public AccountingAccountDto findById(Long id) {
        return repository.findById(id).map(AccountingAccountDto::from)
                .orElseThrow(() -> new AccountingAccountNotFoundException(id));
    }

    @Override
    public AccountingAccountDto findByCode(String code) {
        return repository.findByCode(code).map(AccountingAccountDto::from)
                .orElseThrow(() -> new AccountingAccountNotFoundException(code));
    }
}
