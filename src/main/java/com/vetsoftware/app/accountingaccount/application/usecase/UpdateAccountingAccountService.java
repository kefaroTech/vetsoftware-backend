package com.vetsoftware.app.accountingaccount.application.usecase;

import com.vetsoftware.app.accountingaccount.application.command.UpdateAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import com.vetsoftware.app.accountingaccount.application.port.in.UpdateAccountingAccountUseCase;
import com.vetsoftware.app.accountingaccount.application.port.out.AccountingAccountRepository;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrige el nombre de una cuenta y si exige tercero identificado.
 *
 * <p>
 * <strong>Leer, modificar y guardar es un ciclo con bloqueo optimista.</strong>
 * La entidad lleva {@code @Version}, el dominio conserva ese numero al
 * construir la instancia editada y el {@code save} vuelve con el en el
 * {@code WHERE}: dos ediciones concurrentes no se pisan. Todo dentro de una
 * transaccion, o la lectura y la escritura serian dos operaciones sin nada en
 * medio.
 */
@Observed(name = "accounting.account.update")
@Service
public class UpdateAccountingAccountService implements UpdateAccountingAccountUseCase {

    private final AccountingAccountRepository repository;

    public UpdateAccountingAccountService(AccountingAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AccountingAccountDto execute(UpdateAccountingAccountCommand command) {
        AccountingAccount account = repository.findById(command.id())
                .orElseThrow(() -> new AccountingAccountNotFoundException(command.id()));
        return AccountingAccountDto.from(
                repository.save(account.update(command.name(), command.requiresThirdParty())));
    }
}
