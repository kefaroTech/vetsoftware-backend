package com.vetsoftware.app.accountingaccount.application.port.out;

import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>Ningun metodo de este puerto recibe {@code companyId}, y esa ausencia
 * es una afirmacion sobre el modelo, no un olvido.</strong>
 * {@code accounting_accounts} no tiene columna de empresa: el plan de cuentas
 * es de VetSoftware. Anadir aqui un {@code findAllByCompanyId} exigiria
 * inventarse la columna.
 */
public interface AccountingAccountRepository {

    AccountingAccount save(AccountingAccount account);

    Optional<AccountingAccount> findById(Long id);

    /**
     * Por su codigo, que es la clave natural: es asi como la nombran las tres
     * claves foraneas de {@code account_mappings}.
     */
    Optional<AccountingAccount> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * El plan completo, paginado. No hay hermano acotado por empresa porque no hay
     * empresa: la lista es la misma para toda la plataforma.
     */
    PageResult<AccountingAccount> findAllEnabled(int page, int pageSize);
}
