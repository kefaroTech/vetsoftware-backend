package com.vetsoftware.app.accountingaccount.infrastructure.persistence;

import com.vetsoftware.app.accountingaccount.application.port.out.AccountingAccountRepository;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAccountingAccountRepository implements AccountingAccountRepository {

    private final AccountingAccountJpaRepository jpaRepository;
    private final AccountingAccountJpaMapper mapper;

    public JpaAccountingAccountRepository(AccountingAccountJpaRepository jpaRepository,
            AccountingAccountJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}.</strong> Spring Data no
     * incrementa {@code @Version} hasta el flush, asi que un {@code save} pelado
     * devolveria la entidad con la version <em>anterior</em> y el DTO publicaria un
     * numero que ya no es el de la fila. Aqui el DTO no expone la version, pero el
     * flush es ademas lo que hace que la violacion de
     * {@code uq_accounting_accounts_code} salga <em>dentro</em> del caso de uso y
     * no al cerrar la transaccion, donde ya no hay quien la traduzca.
     */
    @Override
    public AccountingAccount save(AccountingAccount account) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(account)));
    }

    @Override
    public Optional<AccountingAccount> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AccountingAccount> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return code != null && jpaRepository.existsByCode(code);
    }

    @Override
    public PageResult<AccountingAccount> findAllEnabled(int page, int pageSize) {
        return Pages.result(
                jpaRepository.findAllByEnabledTrue(Pages.request(page, pageSize, planOrder())),
                mapper::toDomain);
    }

    /**
     * El plan se lee por codigo, que es como se imprime: el orden del codigo
     * <b>es</b> el orden jerarquico del plan. El {@code id} desempata porque
     * {@code uq_accounting_accounts_code} lo hace imposible hoy, pero un orden que
     * depende de una constraint que un changeset futuro puede mover es un orden que
     * repite u omite filas entre dos paginas consecutivas.
     */
    private static Sort planOrder() {
        return Sort.by(Sort.Order.asc("code"), Sort.Order.asc("id"));
    }
}
