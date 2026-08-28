package com.vetsoftware.app.accountingaccount.application.usecase;

import com.vetsoftware.app.accountingaccount.application.command.CloseAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import com.vetsoftware.app.accountingaccount.application.port.in.CloseAccountingAccountUseCase;
import com.vetsoftware.app.accountingaccount.application.port.out.AccountingAccountRepository;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pone fecha de fin a la vigencia de una cuenta.
 *
 * <p>
 * Que la cuenta no estuviera ya cerrada lo decide el dominio, no este metodo:
 * es una invariante de la cuenta y la base <b>no la cuida</b> —
 * {@code chk_accounting_accounts_validity} solo mira que la fecha de fin sea
 * posterior a la de inicio—.
 */
@Observed(name = "accounting.account.close")
@Service
public class CloseAccountingAccountService implements CloseAccountingAccountUseCase {

    private final AccountingAccountRepository repository;

    public CloseAccountingAccountService(AccountingAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AccountingAccountDto execute(CloseAccountingAccountCommand command) {
        AccountingAccount account = repository.findById(command.id())
                .orElseThrow(() -> new AccountingAccountNotFoundException(command.id()));
        return AccountingAccountDto.from(repository.save(account.close(command.validTo())));
    }
}
