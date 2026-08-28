package com.vetsoftware.app.accountingaccount.application.usecase;

import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import com.vetsoftware.app.accountingaccount.application.port.in.ListAccountingAccountsUseCase;
import com.vetsoftware.app.accountingaccount.application.port.out.AccountingAccountRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * El plan de cuentas, paginado.
 *
 * <p>
 * <strong>No hay hermano acotado por empresa, y no es un olvido.</strong> En
 * las features multi-tenant el par existe porque el listado del tenant esta
 * acotado y el de plataforma no; aqui los dos serian la misma consulta sobre
 * una tabla sin empresa. El puerto va cerrado a {@code hasRole('SYSTEM')} por
 * eso mismo.
 */
@Observed(name = "accounting.account.list")
@Service
public class ListAccountingAccountsService implements ListAccountingAccountsUseCase {

    private final AccountingAccountRepository repository;

    public ListAccountingAccountsService(AccountingAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<AccountingAccountDto> listAll(int page, int pageSize) {
        return repository.findAllEnabled(page, pageSize).map(AccountingAccountDto::from);
    }
}
