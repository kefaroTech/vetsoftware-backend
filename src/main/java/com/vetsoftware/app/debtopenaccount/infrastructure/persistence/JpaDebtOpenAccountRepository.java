package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDebtOpenAccountRepository implements DebtOpenAccountRepository {

    private final DebtOpenAccountJpaRepository jpaRepository;
    private final DebtOpenAccountJpaMapper mapper;
    private final OpenAccountJpaRepository openAccountJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaDebtOpenAccountRepository(DebtOpenAccountJpaRepository jpaRepository,
            DebtOpenAccountJpaMapper mapper, OpenAccountJpaRepository openAccountJpaRepository,
            EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.openAccountJpaRepository = openAccountJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public DebtOpenAccount save(DebtOpenAccount debtOpenAccount) {
        OpenAccountJpaEntity openAccount = openAccountJpaRepository
                .getReferenceById(debtOpenAccount.getOpenAccount().id());
        EmployeeJpaEntity createdBy = employeeJpaRepository
                .getReferenceById(debtOpenAccount.getCreatedBy().id());
        EmployeeJpaEntity voidedBy = debtOpenAccount.getVoidedBy() == null
                ? null
                : employeeJpaRepository.getReferenceById(debtOpenAccount.getVoidedBy().id());
        DebtOpenAccountJpaEntity saved = jpaRepository
                .save(mapper.toJpa(debtOpenAccount, openAccount, createdBy, voidedBy));
        return mapper.toDomain(saved, debtOpenAccount.getOpenAccount(),
                debtOpenAccount.getCreatedBy(), debtOpenAccount.getVoidedBy());
    }

    @Override
    public Optional<DebtOpenAccount> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DebtOpenAccount> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndOpenAccount_Company_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<Long> lockAndFindOpenAccountId(Long id) {
        // getOpenAccount() devuelve el proxy perezoso y getId() lee su identificador
        // sin inicializarlo: ni una consulta mas, y la cuenta NO entra al contexto de
        // persistencia con valores anteriores al lock.
        return jpaRepository.findByIdForUpdate(id).map(e -> e.getOpenAccount().getId());
    }

    @Override
    public Optional<DebtOpenAccount> findByOpenAccountIdAndClientRequestId(Long openAccountId,
            String clientRequestId) {
        return jpaRepository.findByOpenAccount_IdAndClientRequestId(openAccountId, clientRequestId)
                .map(mapper::toDomain);
    }

    @Override
    public List<DebtOpenAccount> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<DebtOpenAccount> findAllByCompanyId(Long companyId, int page, int pageSize) {
        // El orden por id descendente es estable y devuelve primero lo mas reciente;
        // sin orden explicito la paginacion no es determinista y una misma fila puede
        // salir en dos paginas.
        Sort order = Sort.by(Sort.Direction.DESC, "id");
        Page<DebtOpenAccountJpaEntity> result = jpaRepository
                .findAllByOpenAccount_Company_Id(companyId, Pages.request(page, pageSize, order));
        return Pages.result(result, mapper::toDomain);
    }

    @Override
    public List<DebtOpenAccount> findByOpenAccountIdAndCompanyId(Long openAccountId,
            Long companyId) {
        return jpaRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(openAccountId, companyId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id, Long companyId) {
        jpaRepository.findByIdAndOpenAccount_Company_Id(id, companyId)
                .ifPresent(jpaRepository::delete);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }

    @Override
    public Optional<Long> lockAndFindOpenAccountIdIncludingDisabled(Long id) {
        // Mismo criterio que lockAndFindOpenAccountId: getOpenAccount() devuelve el
        // proxy perezoso y getId() lee su identificador sin inicializarlo, asi que la
        // cuenta NO entra al contexto de persistencia con valores anteriores al lock.
        return jpaRepository.findByIdForUpdateIncludingDisabled(id)
                .map(e -> e.getOpenAccount().getId());
    }

    @Override
    public Optional<DebtOpenAccount> findByIdIncludingDisabledAndCompanyId(Long id,
            Long companyId) {
        return jpaRepository.findByIdIncludingDisabledAndCompanyId(id, companyId)
                .map(mapper::toDomain);
    }
}
